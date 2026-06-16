package com.ace3i.connect.service.impl;


import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DottedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.io.font.otf.GlyphLine;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.splitting.DefaultSplitCharacters;
import com.itextpdf.layout.splitting.ISplitCharacters;
import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.entity.Client;
import com.ace3i.connect.entity.ClientDeployment;
import com.ace3i.connect.entity.ClientDeploymentFeature;
import com.ace3i.connect.entity.Mission;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.repository.ClientRepository;
import com.ace3i.connect.repository.MissionRepository;
import com.ace3i.connect.service.IClientFeatureTrackingService;
import com.ace3i.connect.service.IMissionService;
import com.ace3i.connect.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MissionService implements IMissionService {

    // Stratégie pour forcer le retour à la ligne même sur les mots longs sans espaces
    private static final ISplitCharacters SPLIT_ALL = new DefaultSplitCharacters() {
        @Override
        public boolean isSplitCharacter(GlyphLine text, int characterIndex) {
            return true;
        }
    };

    // Couleurs professionnelles
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(238, 119, 37);      // Orange ACE3i
    private static final DeviceRgb COLOR_SECONDARY = new DeviceRgb(103, 190, 217);   // Bleu ACE3i
    private static final DeviceRgb COLOR_SUCCESS = new DeviceRgb(40, 167, 69);       // Vert succès
    private static final DeviceRgb COLOR_WARNING = new DeviceRgb(255, 193, 7);       // Jaune avertissement
    private static final DeviceRgb COLOR_INFO = new DeviceRgb(23, 162, 184);         // Cyan info
    private static final DeviceRgb COLOR_DARK = new DeviceRgb(52, 58, 64);           // Gris foncé
    private static final DeviceRgb COLOR_GRAY = new DeviceRgb(108, 117, 125);        // Gris moyen
    private static final DeviceRgb COLOR_LIGHT = new DeviceRgb(248, 249, 250);       // Gris très clair
    private static final DeviceRgb COLOR_WHITE = new DeviceRgb(255, 255, 255);       // Blanc

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private AppConfigRepository configRepository;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IClientFeatureTrackingService clientFeatureTrackingService;

    @Autowired
    private ClientRepository clientRepository;

    // ========================================================================
    // MÉTHODES CRUD
    // ========================================================================

    @Override
    public List<Mission> getAllMissions() {
        return missionRepository.findAll();
    }

    @Override
    public Page<Mission> getMissions(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        return missionRepository.searchMissions(keyword, status, pageable);
    }

    @Override
    public Optional<Mission> getMissionById(Long id) {
        return missionRepository.findById(id);
    }

    @Override
    public Mission saveMission(Mission mission) {
        boolean isNew = mission.getId() == null;
        Mission savedMission = missionRepository.save(mission);
        if (isNew) {
            notificationService.createNotification(
                    "Nouvelle Mission",
                    "Une nouvelle mission pour le client " + savedMission.getClientName() + " a été créée par " + savedMission.getIntervener(),
                    "success",
                    "/missions/edit/" + savedMission.getId()
            );
        }
        return savedMission;
    }

    @Override
    public void deleteMission(Long id) {
        missionRepository.deleteById(id);
    }

    @Override
    public List<Mission> getMissionsByClientAndPeriod(String clientName, LocalDateTime start, LocalDateTime end) {
        return missionRepository.findByClientAndPeriod(clientName, start, end);
    }



    // ========================================================================
    // 1. FICHE DE PRESTATION (ORIGINALE CONSERVÉE)
    // ========================================================================

    @Override
    public byte[] generateMissionPdf(Mission mission) throws IOException {
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Ajout du pied de page et filigrane via un handler
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            PdfCanvas canvas = new PdfCanvas(page);
            Rectangle pageSize = page.getPageSize();
            Canvas layoutCanvas = new Canvas(canvas, pageSize);

            addWatermark(layoutCanvas, config, pageSize);
            addFooter(layoutCanvas, config, pdf, page, pageSize);

            layoutCanvas.close();
        });

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(15, 25, 40, 25);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{30, 40, 30})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        headerTable.addCell(createLogoCell(config.getLogoLeft(), config.getCompanyName(), TextAlignment.LEFT, HorizontalAlignment.LEFT));

        Cell title = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.BOTTOM);
        title.add(new Paragraph("FICHE DE PRESTATION").setBold().setFontSize(11).setFontColor(COLOR_PRIMARY));
        title.add(new Paragraph("PROJET " + mission.getProjectName()).setBold().setFontSize(9).setFontColor(new DeviceRgb(81, 81, 81)));
        headerTable.addCell(title);

        headerTable.addCell(createLogoCell(config.getLogoRight(), "CERTIFIED Partner", TextAlignment.RIGHT, HorizontalAlignment.RIGHT));
        document.add(headerTable);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30})).useAllAvailableWidth().setMarginTop(5);
        infoTable.addCell(createLabelCell("Nom du Client :")); infoTable.addCell(createValueCell(mission.getClientName()));
        infoTable.addCell(createLabelCell("Intervenant ACE3i :")); infoTable.addCell(createValueCell(mission.getIntervener()));
        infoTable.addCell(createLabelCell("Contact Banque :")); infoTable.addCell(createValueCell(mission.getClientIntervener()));
        infoTable.addCell(createLabelCell("Ordre mission n° :")); infoTable.addCell(createValueCell(mission.getOrderNumber()));
        infoTable.addCell(createLabelCell("Date de début :")); infoTable.addCell(createValueCell(mission.getStartDate().format(dateFormatter)));
        infoTable.addCell(createLabelCell("Date de fin :")); infoTable.addCell(createValueCell(mission.getEndDate().format(dateFormatter)));
        infoTable.addCell(createLabelCell("Durée prévue :")); infoTable.addCell(createValueCell(mission.getCalculatedDuration()));
        infoTable.addCell(createLabelCell("Type Opération :")); infoTable.addCell(createValueCell(mission.getOperationType()));
        document.add(infoTable);

        document.add(new Paragraph("CALENDRIER D'INTERVENTION").setBold().setFontSize(9).setMarginTop(15).setMarginBottom(5));
        Table cal = new Table(UnitValue.createPercentArray(32)).useAllAvailableWidth();
        cal.addCell(new Cell().add(new Paragraph("Jour")).setFontSize(7).setBackgroundColor(new DeviceRgb(240, 240, 240)).setTextAlignment(TextAlignment.CENTER));
        for (int i = 1; i <= 31; i++) cal.addCell(new Cell().add(new Paragraph(String.valueOf(i)).setFontSize(7)).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(new DeviceRgb(250, 250, 250)));
        cal.addCell(new Cell().add(new Paragraph("Matin")).setFontSize(7).setBackgroundColor(new DeviceRgb(240, 240, 240)));
        for (int d = 1; d <= 31; d++) cal.addCell(createCalendarCell(mission, d, true));
        cal.addCell(new Cell().add(new Paragraph("Après-midi")).setFontSize(7).setBackgroundColor(new DeviceRgb(240, 240, 240)));
        for (int d = 1; d <= 31; d++) cal.addCell(createCalendarCell(mission, d, false));
        document.add(cal);

        Table typeTable = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth().setMarginTop(15);
        Cell profile = new Cell().setPadding(5).add(new Paragraph("PROFIL INTERVENANT").setBold().setFontSize(9).setMarginBottom(5));
        profile.add(new Paragraph((mission.isProfileChefProjet() ? "[x] " : "[  ] ") + "Chef de projet").setFontSize(8));
        profile.add(new Paragraph((mission.isProfileConsultant() ? "[x] " : "[  ] ") + "Consultant").setFontSize(8));
        typeTable.addCell(profile);

        Cell types = new Cell().setPadding(5).add(new Paragraph("TYPE D'INTERVENTION").setBold().setFontSize(9).setMarginBottom(5));
        Table subTypes = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        subTypes.addCell(createCheckboxCell("Analyse", mission.isTypeAnalyse()));
        subTypes.addCell(createCheckboxCell("Formation", mission.isTypeFormation()));
        subTypes.addCell(createCheckboxCell("Maintenance", mission.isTypeMaintenance()));
        subTypes.addCell(createCheckboxCell("Prog.", mission.isTypeProgrammation()));
        subTypes.addCell(createCheckboxCell("Assistance", mission.isTypeAssistance()));
        subTypes.addCell(createCheckboxCell("Installation", mission.isTypeInstallation()));
        subTypes.addCell(createCheckboxCell("Mise en prod.", mission.isTypeMiseEnProduction()));
        subTypes.addCell(createCheckboxCell("Suivi projet", mission.isTypeSuiviProjet()));
        subTypes.addCell(createCheckboxCell("Correctif", Boolean.TRUE.equals(mission.getTypeCorrectif())));
        subTypes.addCell(createCheckboxCell("Incident", Boolean.TRUE.equals(mission.getTypeIncident())));
        subTypes.addCell(createCheckboxCell("Support", Boolean.TRUE.equals(mission.getTypeSupport())));
        subTypes.addCell(createCheckboxCell("Évolution", Boolean.TRUE.equals(mission.getTypeEvolution())));
        types.add(subTypes);
        typeTable.addCell(types);
        document.add(typeTable);

        // Section Analyse du Problème (Affichage optimisé)
        if ((mission.getServiceConcerne() != null && !mission.getServiceConcerne().isEmpty()) ||
            (mission.getProblemDescription() != null && !mission.getProblemDescription().isEmpty())) {
            
            document.add(new Paragraph("CADRE DE L'INTERVENTION / PROBLÉMATIQUE").setBold().setFontSize(9).setMarginTop(12).setMarginBottom(5));
            Table analysisTable = new Table(1).useAllAvailableWidth();
            Cell analysisCell = new Cell().setPadding(8).setBorder(new SolidBorder(new DeviceRgb(220, 220, 220), 0.5f)).setBackgroundColor(new DeviceRgb(252, 252, 252));
            
            if (mission.getServiceConcerne() != null && !mission.getServiceConcerne().isEmpty()) {
                Paragraph p = new Paragraph().add(new com.itextpdf.layout.element.Text("Service Concerné : ").setBold().setFontSize(8)).add(new com.itextpdf.layout.element.Text(mission.getServiceConcerne()).setFontSize(8));
                p.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
                analysisCell.add(p);
            }
            if (mission.getProblemDescription() != null && !mission.getProblemDescription().isEmpty()) {
                Paragraph p = new Paragraph().add(new com.itextpdf.layout.element.Text("Description du problème : ").setBold().setFontSize(8)).add(new com.itextpdf.layout.element.Text(mission.getProblemDescription()).setFontSize(8));
                p.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
                analysisCell.add(p);
            }
            if (mission.getRootCause() != null && !mission.getRootCause().isEmpty()) {
                Paragraph p = new Paragraph().add(new com.itextpdf.layout.element.Text("Cause identifiée : ").setBold().setFontSize(8)).add(new com.itextpdf.layout.element.Text(mission.getRootCause()).setFontSize(8));
                p.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
                analysisCell.add(p);
            }
            // La solution (procédure) est volontairement omise de la fiche de prestation
            
            analysisTable.addCell(analysisCell);
            analysisTable.setKeepTogether(true);
            document.add(analysisTable);
        }

        document.add(new Paragraph("TRAVAUX RÉALISÉS DURANT LA MISSION").setBold().setFontSize(9).setMarginTop(12).setMarginBottom(5));
        Table descTable = new Table(1).useAllAvailableWidth();
        Paragraph descPara = new Paragraph(mission.getDescription());
        descPara.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        descTable.addCell(new Cell().add(descPara).setMinHeight(180).setPadding(8).setFontSize(9).setVerticalAlignment(VerticalAlignment.TOP));
        descTable.setKeepTogether(true);
        document.add(descTable);

        Table signs = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(15);
        signs.setKeepTogether(true);
        signs.addCell(createValidationCell("OBSERVATIONS CLIENT", mission.getClientObservations()));
        signs.addCell(createValidationCell("OBSERVATIONS INTERVENANT", mission.getIntervenerObservations()));
        document.add(signs);

        document.close();
        return baos.toByteArray();
    }

    // ========================================================================
    // 2. RAPPORT PÉRIODIQUE (AVEC PAGE DE GARDE ORIGINALE CONSERVÉE)
    // ========================================================================

    @Override
    public byte[] generatePeriodicReportPdf(String clientName, LocalDateTime start, LocalDateTime end, List<Mission> missions) throws IOException {
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        configurePdfEventHandlers(pdf, config);

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(15, 25, 40, 25);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // PAGE 1: PAGE DE GARDE ORIGINALE
        generateOriginalCoverPage(document, config, clientName, start, end, dateFormatter);
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // PAGE 2: SOMMAIRE
        generateVisualTableOfContents(document);
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // PAGES SUIVANTES: CONTENU ANALYTIQUE MODERNE
        generateModernContentPages(document, config, clientName, start, end, missions, dateFormatter);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Page de garde originale restaurée avec design épuré
     */
    private void generateOriginalCoverPage(Document document, AppConfig config, String clientName,
                                           LocalDateTime start, LocalDateTime end, DateTimeFormatter dateFormatter) {
        DeviceRgb lightGray = new DeviceRgb(245, 245, 245);
        
        addEmptyLines(document, 6);
        
        // Logo centré dans une table de largeur fixe pour garantir sa taille
        if (config.getLogoLeft() != null) {
            Table logoTable = new Table(1).setWidth(80).setHorizontalAlignment(HorizontalAlignment.CENTER);
            Image logo = createImageFromBase64(config.getLogoLeft(), 120);
            if (logo != null) {
                logo.setWidth(UnitValue.createPercentValue(100));
                logoTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER));
                document.add(logoTable);
            }
        }
        
        addEmptyLines(document, 3);
        
        // Boîte de titre avec fond gris clair
        Table titleBox = new Table(1).useAllAvailableWidth();
        Cell titleCell = new Cell().setBackgroundColor(lightGray).setPadding(30).setBorder(Border.NO_BORDER);
        titleCell.add(new Paragraph("RAPPORT D'ACTIVITÉ")
                .setFontSize(28)
                .setBold()
                .setFontColor(COLOR_DARK)
                .setTextAlignment(TextAlignment.CENTER));
        titleCell.add(new Paragraph("PÉRIODIQUE")
                .setFontSize(22)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(-10));
        titleBox.addCell(titleCell);
        document.add(titleBox);
        
        addEmptyLines(document, 4);
        
        // Informations destinataire et période
        Table infoTable = new Table(1).setWidth(UnitValue.createPercentValue(60)).setHorizontalAlignment(HorizontalAlignment.CENTER);
        infoTable.addCell(new Cell().add(new Paragraph("DESTINATAIRE")
                .setFontSize(8).setBold().setFontColor(COLOR_GRAY).setCharacterSpacing(1f))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        infoTable.addCell(new Cell().add(new Paragraph(clientName.toUpperCase())
                .setFontSize(20).setBold().setFontColor(COLOR_DARK))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        
        infoTable.addCell(new Cell().add(new Paragraph("\nPÉRIODE D'INTERVENTION")
                .setFontSize(8).setBold().setFontColor(COLOR_GRAY).setCharacterSpacing(1f))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPaddingTop(20));
        infoTable.addCell(new Cell().add(new Paragraph("Du " + start.format(dateFormatter) + " au " + end.format(dateFormatter))
                .setFontSize(12).setItalic().setFontColor(COLOR_DARK))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        document.add(infoTable);
        
        addEmptyLines(document, 12);
        
        document.add(new Paragraph("Document confidentiel généré par ACE3i Connect")
                .setFontSize(7).setItalic().setTextAlignment(TextAlignment.CENTER).setFontColor(COLOR_GRAY));
        document.add(new Paragraph("Date d'édition : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFontSize(7).setTextAlignment(TextAlignment.CENTER).setFontColor(COLOR_GRAY));
    }

    /**
     * Sommaire visuel
     */
    private void generateVisualTableOfContents(Document document) {
        addEmptyLines(document, 2);

        document.add(new Paragraph("SOMMAIRE")
                .setFontSize(28)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

        Table sections = new Table(UnitValue.createPercentArray(new float[]{80, 20})).useAllAvailableWidth();
        sections.setBorder(Border.NO_BORDER);

        String[][] sectionsList = {
                {"01", "TIMELINE CHRONOLOGIQUE", "Visualisation temporelle des interventions"},
                {"02", "RÉSUMÉ EXÉCUTIF", "Indicateurs clés de performance"},
                {"03", "ANALYSE DE LA CHARGE", "Répartition par projet et type"},
                {"04", "DÉTAIL DES INTERVENTIONS", "Liste exhaustive des travaux réalisés"},
                {"05", "OBSERVATIONS", "Commentaires et recommandations"},
                {"06", "PERSPECTIVES", "Prochaines étapes et objectifs"}
        };

        for (String[] section : sectionsList) {
            Table sectionRow = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15})).useAllAvailableWidth().setBorder(Border.NO_BORDER);

            sectionRow.addCell(new Cell().add(new Paragraph(section[0]).setFontSize(24).setBold().setFontColor(COLOR_PRIMARY))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));

            Table titleBlock = new Table(1).useAllAvailableWidth();
            titleBlock.addCell(new Cell().add(new Paragraph(section[1]).setFontSize(14).setBold().setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER));
            titleBlock.addCell(new Cell().add(new Paragraph(section[2]).setFontSize(9).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER));
            sectionRow.addCell(new Cell().add(titleBlock).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));

            sectionRow.addCell(new Cell().add(new Paragraph("→").setFontSize(18).setFontColor(COLOR_PRIMARY))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE));

            sections.addCell(new Cell().add(sectionRow).setBorder(Border.NO_BORDER).setPaddingBottom(20));
        }

        document.add(sections);
        addEmptyLines(document, 2);

        document.add(new Paragraph("................................................................................................................................................................................")
                .setFontSize(6).setFontColor(new DeviceRgb(200, 200, 200)).setMarginTop(20));
    }

    /**
     * Pages de contenu modernisées
     */
    private void generateModernContentPages(Document document, AppConfig config, String clientName,
                                            LocalDateTime start, LocalDateTime end,
                                            List<Mission> missions, DateTimeFormatter dateFormatter) {
        addModernHeader(document, config);

        // SECTION 1: TIMELINE
        addSectionTitle(document, "01", "TIMELINE CHRONOLOGIQUE", "Visualisation des périodes d'intervention");
        Table timeline = createEnhancedTimeline(start, end, missions);
        timeline.setKeepTogether(true);
        document.add(timeline);

        // SECTION 2: DASHBOARD
        addSectionTitle(document, "02", "RÉSUMÉ EXÉCUTIF", "Indicateurs clés de performance");
        Table dashboard = createModernDashboard(missions);
        dashboard.setKeepTogether(true);
        document.add(dashboard);
        
        Table clientInfo = createClientInformationTable(clientName, start, end, missions, dateFormatter);
        clientInfo.setKeepTogether(true);
        document.add(clientInfo);

        Table featureTracking = createFeatureTrackingSummaryTable(clientName);
        featureTracking.setKeepTogether(true);
        document.add(featureTracking);

        Table clientCharts = createClientFocusedChartsSection(missions);
        clientCharts.setKeepTogether(true);
        document.add(clientCharts);

        // SECTION 3: ANALYSE DE LA CHARGE
        addSectionTitle(document, "03", "ANALYSE DE LA CHARGE", "Répartition détaillée des ressources");
        Table workload = createAdvancedWorkloadAnalysis(missions);
        workload.setKeepTogether(true);
        document.add(workload);

        // SECTION 4: DÉTAIL DES INTERVENTIONS
        addSectionTitle(document, "04", "DÉTAIL DES INTERVENTIONS", "Liste exhaustive des travaux réalisés");
        document.add(createEnhancedInterventionsTable(missions, dateFormatter));

        // SECTION 5 & 6 & SIGNATURES (Groupées pour éviter les coupures)
        Table finalBlock = new Table(1).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        finalBlock.setKeepTogether(true);
        
        finalBlock.addCell(new Cell().add(new Paragraph("05 OBSERVATIONS").setBold().setFontSize(12).setFontColor(COLOR_DARK).setMarginTop(20)).setBorder(Border.NO_BORDER));
        finalBlock.addCell(new Cell().add(createEnhancedCommentsSection()).setBorder(Border.NO_BORDER));
        
        finalBlock.addCell(new Cell().add(new Paragraph("06 PERSPECTIVES").setBold().setFontSize(12).setFontColor(COLOR_DARK).setMarginTop(15)).setBorder(Border.NO_BORDER));
        finalBlock.addCell(new Cell().add(createNextStepsSection()).setBorder(Border.NO_BORDER));
        
        document.add(finalBlock);
        
        addSignatureSection(document);
    }

    private void addModernHeader(Document document, AppConfig config) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{25, 50, 25})).useAllAvailableWidth().setBorder(Border.NO_BORDER);

        if (config.getLogoLeft() != null) {
            Image logo = createImageFromBase64(config.getLogoLeft(), 60);
            if (logo != null) {
                logo.setHeight(45).setWidth(60).setAutoScale(false);
                header.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            } else {
                header.addCell(createEmptyHeaderCell());
            }
        } else {
            header.addCell(createEmptyHeaderCell());
        }

        header.addCell(new Cell().add(new Paragraph("SYNTHÈSE D'ACTIVITÉ")
                        .setBold()
                        .setFontSize(12)
                        .setFontColor(COLOR_PRIMARY)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        if (config.getLogoRight() != null) {
            Image logo = createImageFromBase64(config.getLogoRight(), 60);
            if (logo != null) {
                logo.setHeight(45).setWidth(60).setAutoScale(false);
                header.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE));
            } else {
                header.addCell(createEmptyHeaderCell());
            }
        } else {
            header.addCell(createEmptyHeaderCell());
        }

        document.add(header);

        Table separator = new Table(1).useAllAvailableWidth();
        separator.addCell(new Cell().setBackgroundColor(COLOR_PRIMARY).setHeight(2).setBorder(Border.NO_BORDER));
        document.add(separator);
    }

    private Cell createEmptyHeaderCell() {
        return new Cell().setBorder(Border.NO_BORDER).setHeight(45);
    }

    private void addSectionTitle(Document document, String number, String title, String subtitle) {
        document.add(new Paragraph("\n"));

        Table titleBlock = new Table(UnitValue.createPercentArray(new float[]{10, 90})).useAllAvailableWidth().setBorder(Border.NO_BORDER);

        titleBlock.addCell(new Cell().add(new Paragraph(number).setFontSize(28).setBold().setFontColor(COLOR_PRIMARY))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.TOP));

        Table textBlock = new Table(1).useAllAvailableWidth();
        textBlock.addCell(new Cell().add(new Paragraph(title).setBold().setFontSize(16).setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER));
        textBlock.addCell(new Cell().add(new Paragraph(subtitle).setFontSize(9).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER));
        titleBlock.addCell(new Cell().add(textBlock).setBorder(Border.NO_BORDER));

        document.add(titleBlock);
        document.add(new Paragraph(" ").setMarginBottom(10));
    }

    private Table createEnhancedTimeline(LocalDateTime start, LocalDateTime end, List<Mission> missions) {
        long totalDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        Table container = new Table(1).useAllAvailableWidth();
        Table timelineBar = new Table(UnitValue.createPercentArray((int) Math.min(totalDays, 120))).useAllAvailableWidth();

        for (int i = 0; i < totalDays; i++) {
            LocalDateTime current = start.plusDays(i);
            boolean hasMission = missions.stream().anyMatch(m ->
                    !current.isBefore(m.getStartDate()) && !current.isAfter(m.getEndDate()));

            Cell dayCell = new Cell().setHeight(24).setBorder(Border.NO_BORDER);

            if (hasMission) {
                long missionCount = missions.stream().filter(m ->
                        !current.isBefore(m.getStartDate()) && !current.isAfter(m.getEndDate())).count();
                if (missionCount > 1) {
                    dayCell.setBackgroundColor(new DeviceRgb(25, 105, 44)); // Vert foncé
                } else {
                    dayCell.setBackgroundColor(COLOR_SUCCESS); // Vert succès
                }
            } else {
                dayCell.setBackgroundColor(new DeviceRgb(235, 245, 255)); // Bleu très clair
            }
            timelineBar.addCell(dayCell);
        }

        container.addCell(new Cell().add(timelineBar).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(0));

        Table legend = new Table(UnitValue.createPercentArray(new float[]{30, 30, 40})).useAllAvailableWidth().setMarginTop(8);
        legend.addCell(createLegendItem("Activité", COLOR_SUCCESS));
        legend.addCell(createLegendItem("Haute intensité", new DeviceRgb(25, 105, 44)));
        legend.addCell(createLegendItem("Sans activité", new DeviceRgb(235, 245, 255)));

        Table dates = new Table(2).useAllAvailableWidth().setMarginTop(5);
        dates.addCell(new Cell().add(new Paragraph(start.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH))).setFontSize(8).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER));
        dates.addCell(new Cell().add(new Paragraph(end.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH))).setFontSize(8).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));

        container.addCell(new Cell().add(legend).setBorder(Border.NO_BORDER));
        container.addCell(new Cell().add(dates).setBorder(Border.NO_BORDER));

        return container;
    }

    private Cell createLegendItem(String label, DeviceRgb color) {
        Table item = new Table(UnitValue.createPercentArray(new float[]{20, 80})).setBorder(Border.NO_BORDER);
        item.addCell(new Cell().setBackgroundColor(color).setHeight(10).setWidth(10).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(2)));
        item.addCell(new Cell().add(new Paragraph(label).setFontSize(7).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setPaddingLeft(5));
        return new Cell().add(item).setBorder(Border.NO_BORDER);
    }

    private Table createModernDashboard(List<Mission> missions) {
        long finished = missions.stream().filter(m -> "Terminée".equalsIgnoreCase(m.getStatus()) || "Résolu".equalsIgnoreCase(m.getStatus())).count();
        long ongoing = missions.stream().filter(m -> "En cours".equalsIgnoreCase(m.getStatus())).count();
        long planned = missions.stream().filter(m -> !"Terminée".equalsIgnoreCase(m.getStatus()) && !"Résolu".equalsIgnoreCase(m.getStatus()) && !"En cours".equalsIgnoreCase(m.getStatus())).count();
        double totalDays = missions.stream().mapToDouble(this::calculateMissionDuration).sum();

        // Analyse par type stratégique (Data Analysis)
        double incidentDays = missions.stream().filter(m -> Boolean.TRUE.equals(m.getTypeIncident()) || Boolean.TRUE.equals(m.getTypeCorrectif())).mapToDouble(this::calculateMissionDuration).sum();
        double evolutionDays = missions.stream().filter(m -> Boolean.TRUE.equals(m.getTypeEvolution()) || m.isTypeProgrammation() || m.isTypeAnalyse()).mapToDouble(this::calculateMissionDuration).sum();
        double supportDays = missions.stream().filter(m -> Boolean.TRUE.equals(m.getTypeSupport()) || m.isTypeAssistance() || m.isTypeMaintenance()).mapToDouble(this::calculateMissionDuration).sum();
        double otherDays = totalDays - (incidentDays + evolutionDays + supportDays);

        Table dashboard = new Table(UnitValue.createPercentArray(new float[]{30, 40, 30})).useAllAvailableWidth();

        // Bloc Gauche: KPIs majeurs
        Table leftKpis = new Table(1).useAllAvailableWidth();
        leftKpis.addCell(createKpiCard("TOTAL MISSIONS", String.valueOf(missions.size()), "Unités", COLOR_SECONDARY));
        leftKpis.addCell(createKpiCard("VOLUME HORAIRE", String.format("%.1f", totalDays), "Jours", COLOR_PRIMARY));
        dashboard.addCell(new Cell().add(leftKpis).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        // Bloc Central: Équilibre des Services (Visual Stacked Bar)
        Table balanceCard = new Table(1).useAllAvailableWidth();
        balanceCard.addCell(new Cell().add(new Paragraph("ÉQUILIBRE DES PRESTATIONS").setFontSize(8).setBold().setFontColor(COLOR_GRAY).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER));
        
        if (totalDays > 0) {
            Table stackedBar = new Table(UnitValue.createPercentArray(new float[]{
                (float)(evolutionDays/totalDays*100), (float)(incidentDays/totalDays*100), 
                (float)(supportDays/totalDays*100), (float)(Math.max(0, otherDays)/totalDays*100)
            })).useAllAvailableWidth().setHeight(15).setMarginTop(10);
            
            stackedBar.addCell(new Cell().setBackgroundColor(COLOR_SUCCESS).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(4))); // Evolution
            stackedBar.addCell(new Cell().setBackgroundColor(new DeviceRgb(220, 53, 69)).setBorder(Border.NO_BORDER)); // Incident
            stackedBar.addCell(new Cell().setBackgroundColor(COLOR_WARNING).setBorder(Border.NO_BORDER)); // Support
            stackedBar.addCell(new Cell().setBackgroundColor(COLOR_GRAY).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(4))); // Autre
            balanceCard.addCell(new Cell().add(stackedBar).setBorder(Border.NO_BORDER));
        }

        Table miniLegend = new Table(2).useAllAvailableWidth().setMarginTop(10);
        miniLegend.addCell(createMiniLegendItem("Évolutions", COLOR_SUCCESS));
        miniLegend.addCell(createMiniLegendItem("Incidents", new DeviceRgb(220, 53, 69)));
        miniLegend.addCell(createMiniLegendItem("Support", COLOR_WARNING));
        miniLegend.addCell(createMiniLegendItem("Autres", COLOR_GRAY));
        balanceCard.addCell(new Cell().add(miniLegend).setBorder(Border.NO_BORDER));
        
        dashboard.addCell(new Cell().add(balanceCard).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(10).setBorderRadius(new BorderRadius(8)).setMarginLeft(5).setMarginRight(5));

        // Bloc Droit: Détails Statuts
        Table statusLegend = new Table(1).useAllAvailableWidth();
        statusLegend.addCell(new Cell().add(new Paragraph("STATUT DES TRAVAUX").setFontSize(7).setBold().setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setMarginBottom(5));
        statusLegend.addCell(createStatusLegendRow("Terminées", finished, COLOR_SUCCESS));
        statusLegend.addCell(createStatusLegendRow("En cours", ongoing, COLOR_WARNING));
        statusLegend.addCell(createStatusLegendRow("Planifiées", planned, COLOR_INFO));
        dashboard.addCell(new Cell().add(statusLegend).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(10).setBorderRadius(new BorderRadius(8)).setVerticalAlignment(VerticalAlignment.MIDDLE));

        return dashboard;
    }

    private Cell createKpiCard(String title, String value, String unit, DeviceRgb color) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph(value).setFontSize(18).setBold().setFontColor(color)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        card.addCell(new Cell().add(new Paragraph(title).setFontSize(6).setBold().setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setMarginTop(-5));
        return new Cell().add(card).setBorder(new SolidBorder(COLOR_LIGHT, 0.5f)).setPadding(5).setBorderRadius(new BorderRadius(5)).setMarginBottom(5);
    }

    private Cell createMiniLegendItem(String label, DeviceRgb color) {
        Table item = new Table(UnitValue.createPercentArray(new float[]{20, 80})).setBorder(Border.NO_BORDER);
        item.addCell(new Cell().setBackgroundColor(color).setHeight(5).setWidth(5).setBorderRadius(new BorderRadius(2.5f)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        item.addCell(new Cell().add(new Paragraph(label).setFontSize(6)).setBorder(Border.NO_BORDER).setPaddingLeft(3));
        return new Cell().add(item).setBorder(Border.NO_BORDER);
    }

    private Table createStatusLegendRow(String label, long count, DeviceRgb color) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{15, 65, 20})).useAllAvailableWidth();
        row.addCell(new Cell().setBackgroundColor(color).setHeight(8).setWidth(8).setBorderRadius(new BorderRadius(4)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        row.addCell(new Cell().add(new Paragraph(label).setFontSize(8)).setBorder(Border.NO_BORDER).setPaddingLeft(5));
        row.addCell(new Cell().add(new Paragraph(String.valueOf(count)).setFontSize(8).setBold().setFontColor(color)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        return row;
    }

    private Table createAdvancedWorkloadAnalysis(List<Mission> missions) {
        Table analysis = new Table(UnitValue.createPercentArray(new float[]{45, 55})).useAllAvailableWidth();
        
        // GAUCHE: Diagramme de répartition par Type (Grand Visuel)
        Map<String, Double> typeStats = new LinkedHashMap<>();
        missions.forEach(m -> {
            double d = calculateMissionDuration(m);
            if (m.isTypeAnalyse()) typeStats.merge("Analyse", d, Double::sum);
            if (m.isTypeFormation()) typeStats.merge("Formation", d, Double::sum);
            if (m.isTypeMaintenance()) typeStats.merge("Maintenance", d, Double::sum);
            if (m.isTypeAssistance()) typeStats.merge("Assistance", d, Double::sum);
            if (m.isTypeProgrammation()) typeStats.merge("Programmation", d, Double::sum);
            if (m.isTypeInstallation()) typeStats.merge("Installation", d, Double::sum);
        });

        Table typeCard = new Table(1).useAllAvailableWidth();
        typeCard.addCell(new Cell().add(new Paragraph("RÉPARTITION PAR TYPE D'INTERVENTION").setBold().setFontSize(9).setFontColor(COLOR_DARK).setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER).setMarginBottom(10));
        
        // Barre de distribution multicolore
        double totalType = typeStats.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalType > 0) {
            float[] typeWidths = new float[typeStats.size()];
            int idx = 0;
            for (Double val : typeStats.values()) typeWidths[idx++] = (float)(val/totalType*100);
            
            Table distBar = new Table(UnitValue.createPercentArray(typeWidths)).useAllAvailableWidth().setHeight(15);
            DeviceRgb[] typeColors = {new DeviceRgb(103, 190, 217), COLOR_PRIMARY, new DeviceRgb(80, 80, 80), COLOR_INFO, new DeviceRgb(150, 150, 150), COLOR_SUCCESS};
            idx = 0;
            for (int i = 0; i < typeStats.size(); i++) {
                distBar.addCell(new Cell().setBackgroundColor(typeColors[idx % typeColors.length]).setBorder(Border.NO_BORDER));
                idx++;
            }
            typeCard.addCell(new Cell().add(distBar).setBorder(Border.NO_BORDER).setMarginBottom(15));
            
            // Légende détaillée
            Table typeLegend = new Table(2).useAllAvailableWidth();
            idx = 0;
            for (Map.Entry<String, Double> entry : typeStats.entrySet()) {
                typeLegend.addCell(createTypeLegendRow(entry.getKey(), entry.getValue(), typeColors[idx % typeColors.length]));
                idx++;
            }
            typeCard.addCell(new Cell().add(typeLegend).setBorder(Border.NO_BORDER));
        }
        
        analysis.addCell(new Cell().add(typeCard).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(15).setBorderRadius(new BorderRadius(8)).setMarginRight(10));

        // DROITE: Analyse Valeur vs Support
        Table rightSide = new Table(1).useAllAvailableWidth();
        rightSide.addCell(new Cell().add(createValueVsSupportAnalysisCard(missions)).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(15).setBorderRadius(new BorderRadius(8)));
        analysis.addCell(new Cell().add(rightSide).setBorder(Border.NO_BORDER));

        return analysis;
    }

    private Table createTypeLegendRow(String label, double value, DeviceRgb color) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{15, 85})).useAllAvailableWidth();
        row.addCell(new Cell().setBackgroundColor(color).setHeight(6).setWidth(6).setBorderRadius(new BorderRadius(3)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        row.addCell(new Cell().add(new Paragraph(label + ": " + String.format("%.1fJ", value)).setFontSize(7)).setBorder(Border.NO_BORDER).setPaddingLeft(4));
        return row;
    }

    private Table createExpertAnalysisCard(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("CHARGE PAR EXPERT").setBold().setFontSize(9).setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER).setMarginBottom(5));

        Map<String, Double> daysPerExpert = missions.stream()
                .collect(Collectors.groupingBy(Mission::getIntervener,
                        Collectors.summingDouble(this::calculateMissionDuration)));

        double maxDays = daysPerExpert.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        Table data = new Table(1).useAllAvailableWidth();

        daysPerExpert.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .forEach(entry -> {
                    data.addCell(createSmallChartRow(entry.getKey(), entry.getValue(), 0, (float)(entry.getValue()/maxDays), COLOR_SECONDARY));
                });

        card.addCell(new Cell().add(data).setBorder(Border.NO_BORDER));
        return card;
    }

    private Table createProjectAnalysisCard(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("PAR PROJET").setBold().setFontSize(9).setFontColor(COLOR_DARK).setCharacterSpacing(0.5f)).setBorder(Border.NO_BORDER).setMarginBottom(5));

        Map<String, Double> daysPerProject = missions.stream()
                .collect(Collectors.groupingBy(Mission::getProjectName,
                        Collectors.summingDouble(this::calculateMissionDuration)));

        double totalDays = daysPerProject.values().stream().mapToDouble(Double::doubleValue).sum();
        double maxDays = daysPerProject.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        Table data = new Table(UnitValue.createPercentArray(new float[]{100})).useAllAvailableWidth();

        daysPerProject.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    float percentOfMax = (float) (entry.getValue() / maxDays);
                    double percentOfTotal = totalDays == 0 ? 0 : (entry.getValue() / totalDays) * 100;
                    
                    data.addCell(createSmallChartRow(entry.getKey(), entry.getValue(), percentOfTotal, percentOfMax, COLOR_SECONDARY));
                });

        card.addCell(new Cell().add(data).setBorder(new SolidBorder(COLOR_LIGHT, 0.5f)).setPadding(8).setBorderRadius(new BorderRadius(5)));
        return card;
    }

    private Table createCategoryAnalysisCard(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("PAR TYPE").setBold().setFontSize(9).setFontColor(COLOR_DARK).setCharacterSpacing(0.5f)).setBorder(Border.NO_BORDER).setMarginBottom(5));

        Map<String, Double> stats = new LinkedHashMap<>();
        missions.forEach(m -> {
            double d = calculateMissionDuration(m);
            if (m.isTypeAnalyse()) stats.merge("Analyse", d, Double::sum);
            if (m.isTypeFormation()) stats.merge("Formation", d, Double::sum);
            if (m.isTypeMaintenance()) stats.merge("Maintenance", d, Double::sum);
            if (m.isTypeAssistance()) stats.merge("Assistance", d, Double::sum);
            if (m.isTypeProgrammation()) stats.merge("Programmation", d, Double::sum);
            if (m.isTypeInstallation()) stats.merge("Installation", d, Double::sum);
            if (m.isTypeMiseEnProduction()) stats.merge("Mise en Prod.", d, Double::sum);
            if (m.isTypeSuiviProjet()) stats.merge("Suivi Projet", d, Double::sum);
        });

        double total = stats.values().stream().mapToDouble(Double::doubleValue).sum();
        double max = stats.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        Table data = new Table(UnitValue.createPercentArray(new float[]{100})).useAllAvailableWidth();

        stats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    float percentOfMax = (float) (entry.getValue() / max);
                    double percentOfTotal = total == 0 ? 0 : (entry.getValue() / total) * 100;
                    
                    data.addCell(createSmallChartRow(entry.getKey(), entry.getValue(), percentOfTotal, percentOfMax, COLOR_PRIMARY));
                });

        card.addCell(new Cell().add(data).setBorder(new SolidBorder(COLOR_LIGHT, 0.5f)).setPadding(8).setBorderRadius(new BorderRadius(5)));
        return card;
    }

    private Table createValueVsSupportAnalysisCard(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("VALEUR VS SUPPORT").setBold().setFontSize(9).setFontColor(COLOR_DARK).setCharacterSpacing(0.5f)).setBorder(Border.NO_BORDER).setMarginBottom(5));

        double valueDays = 0;
        double supportDays = 0;

        for (Mission m : missions) {
            double d = calculateMissionDuration(m);
            // Catégorisation stratégique
            if (m.isTypeAnalyse() || m.isTypeProgrammation() || m.isTypeInstallation() || 
                m.isTypeMiseEnProduction() || m.isTypeFormation() || m.isTypeSuiviProjet()) {
                valueDays += d;
            } else if (m.isTypeMaintenance() || m.isTypeAssistance()) {
                supportDays += d;
            }
        }

        double total = valueDays + supportDays;
        double max = Math.max(valueDays, supportDays);
        
        Table data = new Table(UnitValue.createPercentArray(new float[]{100})).useAllAvailableWidth();

        if (valueDays > 0) {
            data.addCell(createSmallChartRow("Valeur (Invest.)", valueDays, (valueDays/total)*100, (float)(valueDays/max), COLOR_SUCCESS));
        }
        if (supportDays > 0) {
            data.addCell(createSmallChartRow("Support (Exploit.)", supportDays, (supportDays/total)*100, (float)(supportDays/max), COLOR_WARNING));
        }

        card.addCell(new Cell().add(data).setBorder(new SolidBorder(COLOR_LIGHT, 0.5f)).setPadding(8).setBorderRadius(new BorderRadius(5)));
        
        // Ajout d'une petite note explicative
        card.addCell(new Cell().add(new Paragraph("La Valeur représente l'évolution métier, le Support représente la continuité.")
                .setFontSize(6).setItalic().setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setMarginTop(5));
        
        return card;
    }

    private Cell createSmallChartRow(String label, double value, double percentTotal, float percentMax, DeviceRgb color) {
        Table rowContainer = new Table(1).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        
        Table labelRow = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        labelRow.addCell(new Cell().add(new Paragraph(label).setFontSize(7).setBold()).setBorder(Border.NO_BORDER));
        labelRow.addCell(new Cell().add(new Paragraph(String.format("%.1fJ (%.0f%%)", value, percentTotal)).setFontSize(6).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        
        rowContainer.addCell(new Cell().add(labelRow).setBorder(Border.NO_BORDER).setPaddingTop(2));
        rowContainer.addCell(new Cell().add(createModernProgressBar(percentMax, color)).setBorder(Border.NO_BORDER).setPaddingBottom(3));
        
        return new Cell().add(rowContainer).setBorder(Border.NO_BORDER);
    }

    private Table createModernProgressBar(float percent, DeviceRgb color) {
        float p = Math.max(0.01f, Math.min(1f, percent));
        Table bar = new Table(UnitValue.createPercentArray(new float[]{p * 100, (1 - p) * 100})).useAllAvailableWidth();
        bar.addCell(new Cell().setBackgroundColor(color).setHeight(6).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(3)));
        bar.addCell(new Cell().setBackgroundColor(new DeviceRgb(242, 242, 242)).setHeight(6).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(3)));
        return bar;
    }

    private Table createEnhancedInterventionsTable(List<Mission> missions, DateTimeFormatter formatter) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 22, 40, 18, 10})).useAllAvailableWidth();

        String[] headers = {"DATE", "PROJET", "TRAVAUX RÉALISÉS", "EXPERT", "JOURS"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(8).setFontColor(COLOR_WHITE))
                    .setBackgroundColor(COLOR_DARK)
                    .setPadding(8));
        }

        int rowIndex = 0;
        for (Mission m : missions) {
            DeviceRgb bgColor = (rowIndex % 2 == 0) ? COLOR_WHITE : COLOR_LIGHT;

            table.addCell(createTableCell(m.getStartDate().format(formatter), bgColor));
            table.addCell(createTableCell(m.getProjectName(), bgColor).setBold());

            String desc = m.getDescription();
            if (desc != null && desc.length() > 100) desc = desc.substring(0, 97) + "...";
            table.addCell(createTableCell(desc, bgColor));

            table.addCell(createTableCell(m.getIntervener(), bgColor));
            table.addCell(createTableCell(String.format("%.1f", calculateMissionDuration(m)), bgColor)
                    .setTextAlignment(TextAlignment.CENTER).setBold());

            rowIndex++;
        }
        return table;
    }

    private Cell createTableCell(String text, DeviceRgb bgColor) {
        Paragraph p = new Paragraph(text != null ? text : "").setFontSize(8);
        p.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        return new Cell()
                .add(p)
                .setBackgroundColor(bgColor)
                .setPadding(6);
    }

    private Table createClientInformationTable(String clientName, LocalDateTime start,
                                           LocalDateTime end, List<Mission> missions, DateTimeFormatter dateFormatter) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30})).useAllAvailableWidth().setMarginTop(10).setMarginBottom(10);
        String topExpert = missions.stream()
                .collect(Collectors.groupingBy(Mission::getIntervener, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        infoTable.addCell(createLabelCell("Client :"));
        infoTable.addCell(createValueCell(clientName));
        infoTable.addCell(createLabelCell("Date d'édition :"));
        infoTable.addCell(createValueCell(LocalDateTime.now().format(dateFormatter)));
        infoTable.addCell(createLabelCell("Période :"));
        infoTable.addCell(createValueCell("Du " + start.format(dateFormatter) + " au " + end.format(dateFormatter)));
        infoTable.addCell(createLabelCell("Expert Principal :"));
        infoTable.addCell(createValueCell(topExpert));

        return infoTable;
    }

    private void addClientInformationTable(Document document, String clientName, LocalDateTime start,
                                           LocalDateTime end, List<Mission> missions, DateTimeFormatter dateFormatter) {
        document.add(createClientInformationTable(clientName, start, end, missions, dateFormatter));
    }

    private Table createFeatureTrackingSummaryTable(String clientName) {
        Table card = new Table(1).useAllAvailableWidth().setMarginTop(10).setMarginBottom(10);
        card.setBorder(new SolidBorder(COLOR_LIGHT, 1)).setBorderRadius(new BorderRadius(8));

        Cell title = new Cell()
                .add(new Paragraph("SUIVI VERSION & FONCTIONNALITÉS")
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(COLOR_DARK))
                .setBackgroundColor(COLOR_LIGHT)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
        card.addCell(title);

        Optional<Client> clientOpt = clientRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(clientName))
                .findFirst();

        if (clientOpt.isEmpty()) {
            card.addCell(new Cell()
                    .add(new Paragraph("Aucune donnée de version/fonctionnalités trouvée pour ce client.")
                            .setFontSize(8)
                            .setFontColor(COLOR_GRAY))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8));
            return card;
        }

        Long clientId = clientOpt.get().getId();
        Optional<ClientDeployment> deploymentOpt = clientFeatureTrackingService.getCurrentDeploymentForClient(clientId);
        if (deploymentOpt.isEmpty()) {
            card.addCell(new Cell()
                    .add(new Paragraph("Aucun déploiement applicatif enregistré pour ce client.")
                            .setFontSize(8)
                            .setFontColor(COLOR_GRAY))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8));
            return card;
        }

        ClientDeployment deployment = deploymentOpt.get();
        List<ClientDeploymentFeature> deploymentFeatures = clientFeatureTrackingService.getDeploymentFeaturesForClient(clientId);
        long specificCount = deploymentFeatures.stream()
                .filter(f -> Boolean.TRUE.equals(f.getClientSpecific()))
                .count();
        long globalCount = deploymentFeatures.size() - specificCount;

        Map<String, List<String>> missingAndNew = clientFeatureTrackingService.getMissingAndNewFeaturesForClient(clientId);
        List<String> missingFeatures = missingAndNew.getOrDefault("missing", Collections.emptyList());
        List<String> specificFeatures = deploymentFeatures.stream()
                .filter(f -> Boolean.TRUE.equals(f.getClientSpecific()))
                .map(f -> f.getFeature() != null ? f.getFeature().getName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        Table kpis = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25})).useAllAvailableWidth();
        kpis.addCell(createFeatureKpiCell("Version client", deployment.getApplicationVersion().getVersionNumber(), COLOR_SECONDARY));
        kpis.addCell(createFeatureKpiCell("Déploiement", deployment.getDeploymentDate() != null ? deployment.getDeploymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A", COLOR_INFO));
        kpis.addCell(createFeatureKpiCell("Fonctionnalités globales", String.valueOf(globalCount), COLOR_SUCCESS));
        kpis.addCell(createFeatureKpiCell("Spécificités client", String.valueOf(specificCount), COLOR_PRIMARY));
        card.addCell(new Cell().add(kpis).setBorder(Border.NO_BORDER).setPadding(8));

        Table lists = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        lists.addCell(createFeatureListCell("Fonctionnalités manquantes", missingFeatures, COLOR_WARNING));
        lists.addCell(createFeatureListCell("Fonctionnalités spécifiques client", specificFeatures, COLOR_SECONDARY));
        card.addCell(new Cell().add(lists).setBorder(Border.NO_BORDER).setPadding(8));

        return card;
    }

    private Cell createFeatureKpiCell(String label, String value, DeviceRgb valueColor) {
        Table kpi = new Table(1).useAllAvailableWidth();
        
        Paragraph pLabel = new Paragraph(label).setFontSize(7).setBold().setFontColor(COLOR_GRAY).setTextAlignment(TextAlignment.CENTER);
        pLabel.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        kpi.addCell(new Cell().add(pLabel).setBorder(Border.NO_BORDER));
        
        Paragraph pValue = new Paragraph(value).setFontSize(11).setBold().setFontColor(valueColor).setTextAlignment(TextAlignment.CENTER);
        pValue.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        kpi.addCell(new Cell().add(pValue).setBorder(Border.NO_BORDER));
        
        return new Cell()
                .add(kpi)
                .setBorder(new SolidBorder(COLOR_LIGHT, 0.8f))
                .setBorderRadius(new BorderRadius(5))
                .setPadding(5);
    }

    private Cell createFeatureListCell(String title, List<String> values, DeviceRgb titleColor) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(COLOR_LIGHT, 0.8f))
                .setBorderRadius(new BorderRadius(5))
                .setPadding(8);
                
        Paragraph pTitle = new Paragraph(title).setFontSize(8).setBold().setFontColor(titleColor).setMarginBottom(4);
        pTitle.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        cell.add(pTitle);

        if (values == null || values.isEmpty()) {
            cell.add(new Paragraph("Aucun élément.").setFontSize(7).setFontColor(COLOR_GRAY));
            return cell;
        }

        values.stream().limit(8).forEach(item -> {
                Paragraph pItem = new Paragraph("• " + item).setFontSize(7).setMarginBottom(1);
                pItem.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
                cell.add(pItem);
        });
        if (values.size() > 8) {
            cell.add(new Paragraph("... +" + (values.size() - 8) + " autres").setFontSize(7).setFontColor(COLOR_GRAY));
        }
        return cell;
    }

    private Table createClientFocusedChartsSection(List<Mission> missions) {
        Table section = new Table(1).useAllAvailableWidth().setMarginTop(15).setMarginBottom(15);
        
        // 1. Titre de la section analyse
        section.addCell(new Cell().add(new Paragraph("ANALYSE DÉTAILLÉE DES ACTIVITÉS")
                .setBold().setFontSize(10).setFontColor(COLOR_PRIMARY))
                .setBorder(Border.NO_BORDER).setMarginBottom(10));

        Table chartsGrid = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        // CHART A: Tendance de Charge (Barres Verticales Manuelles)
        chartsGrid.addCell(new Cell().add(createMonthlyTrendVerticalChart(missions)).setBorder(Border.NO_BORDER).setPaddingRight(10));

        // CHART B: Top Projets (Barres Horizontales)
        chartsGrid.addCell(new Cell().add(createTopProjectsHorizontalChart(missions)).setBorder(Border.NO_BORDER));

        section.addCell(new Cell().add(chartsGrid).setBorder(Border.NO_BORDER));
        
        // 3. Répartition Statut & Mode (Plus petit en bas)
        Table miniCharts = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(15);
        miniCharts.addCell(new Cell().add(createSimpleChartCard("DISTRIBUTION PAR TYPE", buildTypeDistributionData(missions), COLOR_PRIMARY)).setBorder(Border.NO_BORDER).setPaddingRight(10));
        miniCharts.addCell(new Cell().add(createSimpleChartCard("STATUTS MISSIONS", buildStatusDistributionData(missions), COLOR_SUCCESS)).setBorder(Border.NO_BORDER));
        
        section.addCell(new Cell().add(miniCharts).setBorder(Border.NO_BORDER));

        return section;
    }

    private Table createMonthlyTrendVerticalChart(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("TENDANCE MENSUELLE (Volume Jours)").setBold().setFontSize(8).setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER).setMarginBottom(10));

        Map<String, Double> data = buildMonthlyDaysData(missions);
        if (data.isEmpty()) {
            card.addCell(new Cell().add(new Paragraph("Aucune donnée disponible").setFontSize(7).setItalic()).setBorder(Border.NO_BORDER));
            return card;
        }

        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        // On dessine l'histogramme avec un tableau
        Table histogram = new Table(UnitValue.createPercentArray(data.size())).useAllAvailableWidth().setHeight(100);
        
        for (Double val : data.values()) {
            float heightPercent = (float)(val / max * 100);
            Cell barCell = new Cell().setVerticalAlignment(VerticalAlignment.BOTTOM).setBorder(Border.NO_BORDER).setPadding(2);
            
            // La barre elle-même
            Table bar = new Table(1).useAllAvailableWidth();
            bar.addCell(new Cell().setBackgroundColor(COLOR_PRIMARY).setHeight(Math.max(1, heightPercent)).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(2)));
            barCell.add(bar);
            
            histogram.addCell(barCell);
        }
        card.addCell(new Cell().add(histogram).setBorder(Border.NO_BORDER).setMarginBottom(5));

        // Labels des mois
        Table labels = new Table(UnitValue.createPercentArray(data.size())).useAllAvailableWidth();
        for (String month : data.keySet()) {
            labels.addCell(new Cell().add(new Paragraph(month).setFontSize(5).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        }
        card.addCell(new Cell().add(labels).setBorder(Border.NO_BORDER));

        return card.setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(10).setBorderRadius(new BorderRadius(8));
    }

    private Table createTopProjectsHorizontalChart(List<Mission> missions) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph("TOP PROJETS (Consommation)").setBold().setFontSize(8).setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER).setMarginBottom(10));

        Map<String, Double> projects = missions.stream()
                .collect(Collectors.groupingBy(Mission::getProjectName, Collectors.summingDouble(this::calculateMissionDuration)));

        List<Map.Entry<String, Double>> topProjects = projects.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toList());

        double max = topProjects.stream().mapToDouble(Map.Entry::getValue).max().orElse(1.0);

        for (Map.Entry<String, Double> entry : topProjects) {
            Table row = new Table(1).useAllAvailableWidth().setMarginBottom(5);
            row.addCell(new Cell().add(new Paragraph(entry.getKey().toUpperCase()).setFontSize(6).setBold()).setBorder(Border.NO_BORDER));
            
            float widthPct = (float)(entry.getValue() / max * 100);
            Table barContainer = new Table(UnitValue.createPercentArray(new float[]{widthPct, 100 - widthPct})).useAllAvailableWidth();
            barContainer.addCell(new Cell().setBackgroundColor(COLOR_SECONDARY).setHeight(6).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(3)));
            barContainer.addCell(new Cell().setBorder(Border.NO_BORDER));
            
            row.addCell(new Cell().add(barContainer).setBorder(Border.NO_BORDER));
            row.addCell(new Cell().add(new Paragraph(String.format("%.1f Jours", entry.getValue())).setFontSize(5).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setMarginTop(-2));
            
            card.addCell(new Cell().add(row).setBorder(Border.NO_BORDER));
        }

        return card.setBorder(new SolidBorder(COLOR_LIGHT, 1)).setPadding(10).setBorderRadius(new BorderRadius(8));
    }

    private Map<String, Double> buildMonthlyDaysData(List<Mission> missions) {
        Map<YearMonth, Double> grouped = new TreeMap<>();
        for (Mission mission : missions) {
            if (mission.getStartDate() == null) continue;
            YearMonth ym = YearMonth.from(mission.getStartDate());
            grouped.put(ym, grouped.getOrDefault(ym, 0.0) + calculateMissionDuration(mission));
        }

        List<Map.Entry<YearMonth, Double>> ordered = new ArrayList<>(grouped.entrySet());
        int from = Math.max(0, ordered.size() - 6);

        Map<String, Double> result = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);
        for (int i = from; i < ordered.size(); i++) {
            result.put(ordered.get(i).getKey().format(fmt), ordered.get(i).getValue());
        }
        return result;
    }

    private Map<String, Double> buildTypeDistributionData(List<Mission> missions) {
        Map<String, Double> stats = new LinkedHashMap<>();
        for (Mission m : missions) {
            double d = calculateMissionDuration(m);
            if (m.isTypeAnalyse()) stats.merge("Analyse", d, Double::sum);
            if (m.isTypeFormation()) stats.merge("Formation", d, Double::sum);
            if (m.isTypeMaintenance()) stats.merge("Maintenance", d, Double::sum);
            if (m.isTypeProgrammation()) stats.merge("Programmation", d, Double::sum);
            if (m.isTypeAssistance()) stats.merge("Assistance", d, Double::sum);
            if (m.isTypeInstallation()) stats.merge("Installation", d, Double::sum);
            if (m.isTypeMiseEnProduction()) stats.merge("Mise en Prod.", d, Double::sum);
            if (m.isTypeSuiviProjet()) stats.merge("Suivi Projet", d, Double::sum);
        }
        return stats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Double> buildStatusDistributionData(List<Mission> missions) {
        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("Terminée", 0.0);
        stats.put("En cours", 0.0);
        stats.put("Planifiée/Autre", 0.0);

        for (Mission m : missions) {
            String status = m.getStatus() == null ? "" : m.getStatus().trim();
            if ("Terminée".equalsIgnoreCase(status) || "Résolu".equalsIgnoreCase(status)) {
                stats.put("Terminée", stats.get("Terminée") + 1.0);
            } else if ("En cours".equalsIgnoreCase(status)) {
                stats.put("En cours", stats.get("En cours") + 1.0);
            } else {
                stats.put("Planifiée/Autre", stats.get("Planifiée/Autre") + 1.0);
            }
        }
        return stats;
    }

    private Cell createSimpleChartCard(String title, Map<String, Double> data, DeviceRgb color) {
        Table card = new Table(1).useAllAvailableWidth();
        card.addCell(new Cell().add(new Paragraph(title).setBold().setFontSize(8).setFontColor(COLOR_DARK)).setBorder(Border.NO_BORDER).setMarginBottom(4));

        if (data == null || data.isEmpty()) {
            card.addCell(new Cell().add(new Paragraph("Aucune donnée").setFontSize(7).setFontColor(COLOR_GRAY)).setBorder(Border.NO_BORDER));
            return new Cell().add(card).setBorder(new SolidBorder(COLOR_LIGHT, 1)).setBorderRadius(new BorderRadius(8)).setPadding(8);
        }

        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();

        Table rows = new Table(1).useAllAvailableWidth();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            float pctMax = max <= 0 ? 0.01f : (float) (entry.getValue() / max);
            double pctTotal = total <= 0 ? 0 : (entry.getValue() / total) * 100.0;
            rows.addCell(createSmallChartRow(entry.getKey(), entry.getValue(), pctTotal, pctMax, color));
        }
        card.addCell(new Cell().add(rows).setBorder(Border.NO_BORDER));

        return new Cell()
                .add(card)
                .setBorder(new SolidBorder(COLOR_LIGHT, 1))
                .setBorderRadius(new BorderRadius(8))
                .setPadding(8);
    }

    private Table createEnhancedCommentsSection() {
        Table comments = new Table(1).useAllAvailableWidth();
        Cell obsCell = new Cell()
                .setMinHeight(100)
                .setPadding(15)
                .setBorder(new DottedBorder(COLOR_GRAY, 0.5f))
                .setBorderRadius(new BorderRadius(8))
                .add(new Paragraph("Observations globales de la période :")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(COLOR_DARK))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("_____________________________________________________________")
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("_____________________________________________________________")
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("_____________________________________________________________")
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY));
        comments.addCell(obsCell);
        return comments;
    }

    private Table createNextStepsSection() {
        Table nextSteps = new Table(1).useAllAvailableWidth();
        Cell stepsCell = new Cell()
                .setMinHeight(80)
                .setPadding(15)
                .setBackgroundColor(COLOR_LIGHT)
                .setBorderRadius(new BorderRadius(8))
                .add(new Paragraph("Prochaines interventions prévues :")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(COLOR_PRIMARY))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("• ")
                        .setFontSize(8))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("• ")
                        .setFontSize(8))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("• ")
                        .setFontSize(8))
                .add(new Paragraph("\n\nObjectifs majeurs pour la prochaine période :")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(COLOR_DARK))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("→ ")
                        .setFontSize(8))
                .add(new Paragraph("\n")
                        .setFontSize(8))
                .add(new Paragraph("→ ")
                        .setFontSize(8));
        nextSteps.addCell(stepsCell);
        return nextSteps;
    }

    private void addSignatureSection(Document document) {
        document.add(new Paragraph("\n"));
        Table signature = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(30);
        signature.addCell(new Cell()
                .add(new Paragraph("Pour ACE3i Afrique")
                        .setBold()
                        .setFontSize(9))
                .add(new Paragraph("\n\n\n")
                        .setFontSize(8))
                .add(new Paragraph("(Cachet et Signature)")
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY))
                .setBorder(Border.NO_BORDER));
        signature.addCell(new Cell()
                .add(new Paragraph("Pour le Client")
                        .setBold()
                        .setFontSize(9))
                .add(new Paragraph("\n\n\n")
                        .setFontSize(8))
                .add(new Paragraph("(Visa)")
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT));
        document.add(signature);
        Table footerLine = new Table(1).useAllAvailableWidth().setMarginTop(20);
        footerLine.addCell(new Cell().setBackgroundColor(COLOR_PRIMARY).setHeight(1).setBorder(Border.NO_BORDER));
        document.add(footerLine);
    }

    // ========================================================================
    // MÉTHODES UTILITAIRES COMMUNES
    // ========================================================================

    private double calculateMissionDuration(Mission m) {
        if (m.getStartDate() == null || m.getEndDate() == null) return 0.0;
        // Calcul inclusif des jours
        return (double) java.time.temporal.ChronoUnit.DAYS.between(m.getStartDate().toLocalDate(), m.getEndDate().toLocalDate()) + 1.0;
    }

    private void configurePdfEventHandlers(PdfDocument pdf, AppConfig config) {
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            if (pdf.getPageNumber(docEvent.getPage()) == 1) return;

            PdfPage page = docEvent.getPage();
            PdfCanvas canvas = new PdfCanvas(page);
            Rectangle pageSize = page.getPageSize();
            Canvas layoutCanvas = new Canvas(canvas, pageSize);

            addWatermark(layoutCanvas, config, pageSize);
            addFooter(layoutCanvas, config, pdf, page, pageSize);

            layoutCanvas.close();
        });
    }

    private void addWatermark(Canvas canvas, AppConfig config, Rectangle pageSize) {
        if (config.getWatermarkImage() != null) {
            Image watermark = createImageFromBase64(config.getWatermarkImage(), 500);
            if (watermark != null) {
                float x = (pageSize.getLeft() + pageSize.getRight()) / 2;
                float y = (pageSize.getTop() + pageSize.getBottom()) / 2;
                float imgWidth = 80;
                float imgHeight = watermark.getImageHeight() * (imgWidth / watermark.getImageWidth());
                watermark.setOpacity(0.1f);
                watermark.setFixedPosition(x - (imgWidth / 2) - 250, y - (imgHeight / 2) - 220, imgWidth);
                canvas.add(watermark);
            }
        }
    }

    private void addFooter(Canvas canvas, AppConfig config, PdfDocument pdf, PdfPage page, Rectangle pageSize) {
        float footerY = 20;
        float centerX = pageSize.getWidth() / 2;

        String companyInfo = (config.getCompanyName() != null ? config.getCompanyName() : "ACE3i AFRICA") +
                (config.getAddress() != null ? " - " + config.getAddress() : "");
        canvas.showTextAligned(new Paragraph(companyInfo).setFontSize(7).setFontColor(COLOR_GRAY),
                centerX, footerY + 8, TextAlignment.CENTER);

        String contactInfo = (config.getPhone() != null ? "Tel: " + config.getPhone() : "") +
                (config.getEmail() != null ? " | Email: " + config.getEmail() : "");
        canvas.showTextAligned(new Paragraph(contactInfo).setFontSize(7).setFontColor(COLOR_GRAY),
                centerX, footerY, TextAlignment.CENTER);

        int currentPage = pdf.getPageNumber(page);
        int totalPages = pdf.getNumberOfPages();

        if (totalPages > 2 && currentPage > 1) {
            canvas.showTextAligned(new Paragraph("Page " + (currentPage - 1) + " / " + (totalPages - 1))
                            .setFontSize(7).setFontColor(COLOR_GRAY),
                    pageSize.getWidth() - 25, footerY, TextAlignment.RIGHT);
        }
    }

    private void addEmptyLines(Document document, int lines) {
        for (int i = 0; i < lines; i++) {
            document.add(new Paragraph("\n"));
        }
    }

    private Image createImageFromBase64(String base64, float maxWidth) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            String pureBase64 = base64.contains(",") ? base64.split(",")[1] : base64;
            byte[] bytes = Base64.getDecoder().decode(pureBase64.trim());
            Image img = new Image(ImageDataFactory.create(bytes));
            img.setMaxWidth(maxWidth);
            img.setAutoScale(true);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private Cell createLabelCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(10)).setBorder(Border.NO_BORDER);
    }

    private Cell createValueCell(String text) {
        Paragraph p = new Paragraph(text != null ? text : "").setFontSize(10).setUnderline();
        p.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        return new Cell().add(p).setBorder(Border.NO_BORDER);
    }

    private Cell createCheckboxCell(String label, boolean checked) {
        return new Cell().add(new Paragraph((checked ? "[x] " : "[  ] ") + label).setFontSize(8)).setBorder(Border.NO_BORDER);
    }

    private Cell createCalendarCell(Mission mission, int day, boolean isAM) {
        Cell cell = new Cell().setTextAlignment(TextAlignment.CENTER).setFontSize(8);
        if (mission.getStartDate() == null || mission.getEndDate() == null) return cell;
        try {
            LocalDateTime start = mission.getStartDate();
            LocalDateTime end = mission.getEndDate();
            LocalDateTime currentDayStart = start.withDayOfMonth(day).withHour(0).withMinute(0);
            LocalDateTime currentDayMid = start.withDayOfMonth(day).withHour(12).withMinute(0);
            LocalDateTime currentDayEnd = start.withDayOfMonth(day).withHour(23).withMinute(59);
            boolean active = isAM ? (start.isBefore(currentDayMid) && end.isAfter(currentDayStart)) : (start.isBefore(currentDayEnd) && end.isAfter(currentDayMid));
            if (active) {
                cell.add(new Paragraph("X").setBold());
                cell.setBackgroundColor(new DeviceRgb(220, 230, 241));
            }
        } catch (Exception e) {
            cell.setBackgroundColor(new DeviceRgb(230, 230, 240));
        }
        return cell;
    }

    private Cell createValidationCell(String title, String obs) {
        Cell cell = new Cell().setPadding(5).setBorder(new SolidBorder(new DeviceRgb(200, 200, 200), 0.5f)).setMinHeight(100);
        cell.add(new Paragraph(title).setBold().setUnderline().setFontSize(9).setMarginBottom(5));
        Paragraph pObs = new Paragraph(obs != null ? obs : "").setFontSize(8).setMultipliedLeading(1.2f).setTextAlignment(TextAlignment.LEFT);
        pObs.setProperty(Property.SPLIT_CHARACTERS, SPLIT_ALL);
        cell.add(pObs);
        String dt = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Table f = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(10);
        f.addCell(new Cell().add(new Paragraph("Date: " + dt)).setBorder(Border.NO_BORDER).setFontSize(8));
        f.addCell(new Cell().add(new Paragraph("Signature:")).setBorder(Border.NO_BORDER).setFontSize(8));
        return cell.add(f);
    }

    private Cell createLogoCell(String logoBase64, String defaultText, TextAlignment alignment, HorizontalAlignment imageAlignment) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setTextAlignment(alignment).setVerticalAlignment(VerticalAlignment.BOTTOM);
        if (logoBase64 != null) {
            Image logo = createImageFromBase64(logoBase64, 80);
            if (logo != null) {
                logo.setHeight(60).setWidth(80).setAutoScale(false).setHorizontalAlignment(imageAlignment);
                cell.add(logo);
            } else {
                cell.add(new Paragraph(defaultText).setBold().setFontSize(9).setFontColor(COLOR_SECONDARY));
            }
        } else {
            cell.add(new Paragraph(defaultText).setBold().setFontSize(9).setFontColor(COLOR_SECONDARY));
        }
        return cell;
    }
}