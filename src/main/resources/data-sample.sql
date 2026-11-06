-- =========================================================================================
-- ACE3i Connect - Jeu de Données Réel
-- Banques : SUNU, IB-BANK, UTB, CORIS, ORABANK
-- Catégories : EPAIEMENT, MBANKING, AMPLITUDE, SEGUCE, OTR
-- =========================================================================================

-- 1. Insertion / Mise à niveau des Banques partenaires
INSERT INTO `cncli` (`clinam`, `clibra`) 
SELECT 'SUNU' WHERE NOT EXISTS (SELECT 1 FROM `cncli` WHERE `clinam` = 'SUNU');
INSERT INTO `cncli` (`clinam`, `clibra`) 
SELECT 'IB-BANK' WHERE NOT EXISTS (SELECT 1 FROM `cncli` WHERE `clinam` = 'IB-BANK');
INSERT INTO `cncli` (`clinam`, `clibra`) 
SELECT 'UTB' WHERE NOT EXISTS (SELECT 1 FROM `cncli` WHERE `clinam` = 'UTB');
INSERT INTO `cncli` (`clinam`, `clibra`) 
SELECT 'CORIS' WHERE NOT EXISTS (SELECT 1 FROM `cncli` WHERE `clinam` = 'CORIS');
INSERT INTO `cncli` (`clinam`, `clibra`) 
SELECT 'ORABANK' WHERE NOT EXISTS (SELECT 1 FROM `cncli` WHERE `clinam` = 'ORABANK');

-- 2. Insertion / Mise à niveau des 5 Catégories de Tickets officielles
INSERT INTO `cntca` (`tcanam`, `tcaord`, `tcaact`) 
SELECT 'EPAIEMENT', 1, 1 WHERE NOT EXISTS (SELECT 1 FROM `cntca` WHERE `tcanam` = 'EPAIEMENT');
INSERT INTO `cntca` (`tcanam`, `tcaord`, `tcaact`) 
SELECT 'MBANKING', 2, 1 WHERE NOT EXISTS (SELECT 1 FROM `cntca` WHERE `tcanam` = 'MBANKING');
INSERT INTO `cntca` (`tcanam`, `tcaord`, `tcaact`) 
SELECT 'AMPLITUDE', 3, 1 WHERE NOT EXISTS (SELECT 1 FROM `cntca` WHERE `tcanam` = 'AMPLITUDE');
INSERT INTO `cntca` (`tcanam`, `tcaord`, `tcaact`) 
SELECT 'SEGUCE', 4, 1 WHERE NOT EXISTS (SELECT 1 FROM `cntca` WHERE `tcanam` = 'SEGUCE');
INSERT INTO `cntca` (`tcanam`, `tcaord`, `tcaact`) 
SELECT 'OTR', 5, 1 WHERE NOT EXISTS (SELECT 1 FROM `cntca` WHERE `tcanam` = 'OTR');

-- Récupération dynamique d'un Client ID et d'un User ID valides
SET @default_client_id = (SELECT `cliid` FROM `cncli` ORDER BY `cliid` ASC LIMIT 1);
SET @default_user_id   = (SELECT `useid` FROM `cnuse` ORDER BY `useid` ASC LIMIT 1);

-- =========================================================================================
-- 3. INSERTION DE 25 MISSIONS D'INTERVENTION
-- =========================================================================================

INSERT INTO `cnmis` (
    `misprj`, `misord`, `miscli`, `misint`, `misicl`,
    `misdeb`, `misfin`, `misday`, `mistyp`, `missta`, `misopr`,
    `misotn`, `misotl`, `mispch`, `mispco`,
    `mistan`, `mistfo`, `mistma`, `mistpr`, `mistas`, `mistin`, `mistme`, `mistsu`,
    `mistco`, `mistic`, `mistsp`, `mistev`, `misser`,
    `mispds`, `miscau`, `missol`, `misdes`, `misocl`, `misoin`
) VALUES
('AMPLITUDE Core Banking', CONCAT('ORD-', UNIX_TIMESTAMP(), '-01'), 'SUNU', 'Koffi Mensah', 'M. Yao (DSI)', 
 '2026-10-01 08:30:00', '2026-10-01 17:30:00', 1.0, 'Maintenance', 'Terminée', 'En présentiel',
 'OP-1021', 'http://openproject.ace3i.local/wp/1021', 1, 0, 
 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 'Exploitation',
 'Ralentissement lors des clôtures journalières AMPLITUDE', 'Index corrompu sur la table des écritures comptables', 'Reconstruction des index et optimisation du plan de requêtes MySQL', 'Optimisation exécutée avec succès, temps de traitement réduit de 45 minutes.', 'Performance satisfaisante constatée.', 'Prévoir un contrôle de la purge le mois prochain.'),

('Passerelle OTR Télécollecte', CONCAT('ORD-', UNIX_TIMESTAMP(), '-02'), 'CORIS', 'Jean-Luc Amegan', 'Mme Traoré', 
 '2026-10-03 09:00:00', '2026-10-04 18:00:00', 2.0, 'Installation', 'Terminée', 'En présentiel',
 'OP-1025', 'http://openproject.ace3i.local/wp/1025', 0, 1, 
 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 'Fiscalité & Flux',
 'Mise en conformité du connecteur OTR', 'Nouvelles spécifications techniques de l Office Togolais des Recettes', 'Déploiement du connecteur v3.1 et tests d échange avec la plateforme OTR', 'Validation des flux de télépaiement réussie.', 'Conforme aux exigences.', 'R.A.S.'),

('MBANKING Mobile App', CONCAT('ORD-', UNIX_TIMESTAMP(), '-03'), 'ORABANK', 'Ablam Lawson', 'M. Dossou', 
 '2026-10-05 10:00:00', '2026-10-05 16:00:00', 0.75, 'Assistance', 'Terminée', 'En ligne',
 'OP-1030', NULL, 0, 1, 
 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 'Support Digital',
 'Échec d envoi des OTP SMS sur l application mobile', 'Timeout de connexion vers la passerelle Télécom', 'Augmentation du timeout et mise en place d un routeur SMS de secours', 'Basculement automatisé testé avec succès.', 'Service rétabli.', 'Surveillance active.'),

('Plateforme SEGUCE Guichet Unique', CONCAT('ORD-', UNIX_TIMESTAMP(), '-04'), 'UTB', 'Koffi Mensah', 'M. Hounkpati', 
 '2026-10-08 08:00:00', '2026-10-08 17:00:00', 1.0, 'Maintenance', 'Terminée', 'En ligne',
 'OP-1042', NULL, 1, 0, 
 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 'Commerce Extérieur',
 'Intégration des quittances douanières SEGUCE', 'Mise à niveau des webservices SEGUCE Togo', 'Mise à jour des certificats et vérification des files d attente', 'Toutes les quittances portuaires sont synchronisées en temps réel.', 'Opérationnel.', 'R.A.S.'),

('Module Guichet AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-05'), 'IB-BANK', 'Jean-Luc Amegan', 'M. Alassane', 
 '2026-10-10 08:30:00', '2026-10-10 12:30:00', 0.5, 'Formation', 'Terminée', 'En présentiel',
 'OP-1048', NULL, 0, 1, 
 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Réseau Agences',
 'Arrivée de nouveaux caissiers sur AMPLITUDE', 'Besoin de formation sur les opérations de caisse', 'Session pratique sur les modules de versement, retrait et change', 'Formation dispensée et attestée.', 'Personnel opérationnel.', 'Guides utilisateurs remis.'),

('EPAIEMENT Passerelle Web', CONCAT('ORD-', UNIX_TIMESTAMP(), '-06'), 'CORIS', 'Koffi Mensah', 'Mme Kaboré', 
 '2026-10-12 14:00:00', '2026-10-12 18:00:00', 0.5, 'Assistance', 'Terminée', 'En ligne',
 'OP-1055', NULL, 0, 1, 
 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 'Monétique & E-commerce',
 'Erreur de callback sur transactions marchandes', 'URL de notification rejetée par le pare-feu du marchand', 'Ajustement de la règle de sécurité et rejeu des notifications', 'Toutes les transactions en attente ont été validées.', 'Parfait.', 'RAS.'),

('Cluster AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-07'), 'ORABANK', 'Ablam Lawson', 'M. Ayivi', 
 '2026-10-15 22:00:00', '2026-10-16 04:00:00', 0.75, 'Mise en production', 'Terminée', 'En présentiel',
 'OP-1060', NULL, 1, 1, 
 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 'Infrastructures',
 'Bascule nocturne sur le serveur de secours AMPLITUDE', 'Test de bascule semestriel', 'Arrêt propre des services et démarrage sur nœud secondaire', 'Bascule effectuée sans interruption de service client.', 'Reprise normale le matin.', 'Monitoring stable.'),

('Module Crédit AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-08'), 'SUNU', 'Jean-Luc Amegan', 'M. Yao', 
 '2026-10-18 09:00:00', '2026-10-18 17:00:00', 1.0, 'Programmation', 'Terminée', 'En ligne',
 'OP-1068', NULL, 0, 1, 
 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 'Engagements',
 'Ajout d un barème d amortissement spécifique', 'Demande commerciale de la direction générale', 'Développement et intégration du nouveau simulateur de prêt', 'Livré sur environnement de recette.', 'Tests concluants.', 'Passage en prod planifié.'),

('Supervision MBANKING', CONCAT('ORD-', UNIX_TIMESTAMP(), '-09'), 'IB-BANK', 'Koffi Mensah', 'M. Dossou', 
 '2026-10-20 09:00:00', '2026-10-20 15:00:00', 0.75, 'Maintenance', 'Terminée', 'En ligne',
 'OP-1072', NULL, 0, 1, 
 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 'Exploitation',
 'Vérification des alertes disques et logs serveurs', 'Routine préventive mensuelle', 'Nettoyage des archives et purge des tables d audit temporaires', 'Gain de 120 Go d espace disque.', 'Bien reçu.', 'R.A.S.'),

('Portail E-Banking', CONCAT('ORD-', UNIX_TIMESTAMP(), '-10'), 'CORIS', 'Ablam Lawson', 'Mme Traoré', 
 '2026-10-22 08:30:00', '2026-10-23 17:30:00', 2.0, 'Installation', 'Terminée', 'En présentiel',
 'OP-1080', NULL, 1, 1, 
 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 'Banque Digitale',
 'Déploiement de la version 3.2.0', 'Nouvelle charte graphique et ergonomie', 'Installation des paquets web et configuration du reverse proxy Nginx', 'Portail déployé et opérationnel.', 'Excellents retours clients.', 'Formation du helpdesk effectuée.'),

('Connecteur OTR', CONCAT('ORD-', UNIX_TIMESTAMP(), '-11'), 'UTB', 'Jean-Luc Amegan', 'M. Dossou', 
 '2026-10-25 08:00:00', '2026-10-25 12:00:00', 0.5, 'Assistance', 'Terminée', 'En ligne',
 'OP-1085', NULL, 0, 1, 
 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 'Fiscalité & Télécollecte',
 'Ajustement de l API de paiement des taxes douanières OTR', 'Modifications de format des avis d imposition', 'Mise à jour des schémas JSON et validation des quittances', 'Toutes les quittances OTR générées avec succès.', 'Validé.', 'R.A.S.'),

('Plateforme SEGUCE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-12'), 'SUNU', 'Ablam Lawson', 'M. Yao', 
 '2026-10-27 10:00:00', '2026-10-27 16:00:00', 0.75, 'Maintenance', 'Terminée', 'En ligne',
 'OP-1090', NULL, 0, 1, 
 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 'Commerce Extérieur',
 'Certificat SSL de la liaison SEGUCE arrivé à expiration', 'Expiration annuelle', 'Renouvellement et déploiement du certificat SSL Let s Encrypt', 'Notifications rétablies.', 'Vérifié.', 'Automatiser le renouvellement.'),

('Clôture Mensuelle AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-13'), 'IB-BANK', 'Koffi Mensah', 'Mme Kaboré', 
 '2026-10-31 18:00:00', '2026-10-31 23:30:00', 0.7, 'Assistance', 'Terminée', 'En ligne',
 'OP-1099', NULL, 1, 0, 
 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 'Comptabilité',
 'Assistance à l exécution de la clôture comptable d octobre', 'Assistance critique trimestrielle', 'Supervision des jobs de calcul d intérêts et génération des balances', 'Clôture validée sans incident.', 'Comptes clôturés dans les délais.', 'R.A.S.'),

('Plateforme EPAIEMENT', CONCAT('ORD-', UNIX_TIMESTAMP(), '-14'), 'CORIS', 'Jean-Luc Amegan', 'M. Alassane', 
 '2026-11-01 08:30:00', '2026-11-02 18:00:00', 2.0, 'Maintenance', 'Terminée', 'En présentiel',
 'OP-1105', NULL, 0, 1, 
 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 'E-Paiement',
 'Audit de la plateforme e-commerce et passerelles marchandes', 'Vérification de la conformité 3D-Secure', 'Mise à niveau des clés de chiffrement et tests transactionnels', 'Plateforme certifiée.', 'Bon travail.', 'R.A.S.'),

('Module MBANKING', CONCAT('ORD-', UNIX_TIMESTAMP(), '-15'), 'ORABANK', 'Ablam Lawson', 'M. Ayivi', 
 '2026-11-03 09:00:00', '2026-11-03 17:00:00', 1.0, 'Installation', 'En cours', 'En ligne',
 'OP-1110', NULL, 0, 1, 
 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 'Banque Digitale',
 'Intégration du virement instantané sur l application MBANKING', 'Évolution fonctionnelle', 'Déploiement du module en environnement de qualification', 'Recette en cours avec l équipe métier.', 'En attente des retours de validation.', 'Livraison prod prévue vendredi.'),

('Archivage AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-16'), 'SUNU', 'Koffi Mensah', 'M. Yao', 
 '2026-11-04 08:30:00', '2026-11-04 18:00:00', 1.0, 'Maintenance', 'En cours', 'En présentiel',
 'OP-1115', NULL, 1, 0, 
 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Organisation & Méthodes',
 'Projet GED et archivage des pièces comptables', 'Saturation de l espace de stockage local', 'Paramétrage du robot d archivage vers le SAN distant', 'Transfert des archives 2023 en cours.', 'Travaux suivis par l équipe.', 'Reste les années 2024 et 2025.'),

('Sécurité Réseau OTR / SEGUCE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-17'), 'UTB', 'Ablam Lawson', 'M. Dossou', 
 '2026-11-05 10:00:00', '2026-11-05 16:30:00', 0.8, 'Assistance', 'En cours', 'En ligne',
 'OP-1120', NULL, 0, 1, 
 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 'Sécurité SI',
 'Revue des tunnels VPN vers OTR et SEGUCE', 'Audit semestriel de sécurité', 'Filtrage strict des flux entrants et sortants vers les passerelles', 'Analyse des logs en direct.', 'En cours.', 'RAS.'),

('AMPLITUDE Comptabilité', CONCAT('ORD-', UNIX_TIMESTAMP(), '-18'), 'CORIS', 'Jean-Luc Amegan', 'Mme Traoré', 
 '2026-11-07 09:00:00', '2026-11-07 17:00:00', 1.0, 'Formation', 'Planifiée', 'En présentiel',
 'OP-1125', NULL, 0, 1, 
 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Comptabilité',
 'Formation sur les états réglementaires BCEAO sous AMPLITUDE', 'Changement réglementaire', 'Session de formation théorique et cas pratiques dans le logiciel', NULL, NULL, 'Salle de formation réservée.'),

('Serveur d Application MBANKING', CONCAT('ORD-', UNIX_TIMESTAMP(), '-19'), 'ORABANK', 'Koffi Mensah', 'M. Hounkpati', 
 '2026-11-08 20:00:00', '2026-11-09 02:00:00', 0.75, 'Mise en production', 'Planifiée', 'En ligne',
 'OP-1130', NULL, 1, 0, 
 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 'Infrastructures',
 'Déploiement du patch correctif de sécurité Tomcat/Java', 'Correction de vulnérabilités CVE', 'Application des correctifs et redémarrage ordonné des services', NULL, NULL, 'Plan de retour arrière rédigé.'),

('Passerelle EPAIEMENT Télépaiement', CONCAT('ORD-', UNIX_TIMESTAMP(), '-20'), 'SUNU', 'Ablam Lawson', 'M. Alassane', 
 '2026-11-10 09:00:00', '2026-11-12 18:00:00', 3.0, 'Installation', 'Planifiée', 'En présentiel',
 'OP-1135', NULL, 1, 1, 
 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 'Paiements Numériques',
 'Raccordement à la plateforme nationale de télépaiement', 'Projet stratégique 2026', 'Installation de la passerelle API et tests de connectivité', NULL, NULL, 'Documents techniques transmis.'),

('Audit de Performance AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-21'), 'IB-BANK', 'Koffi Mensah', 'M. Yao', 
 '2026-11-14 09:00:00', '2026-11-14 17:00:00', 1.0, 'Analyse', 'Planifiée', 'En ligne',
 'OP-1140', NULL, 1, 0, 
 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Exploitation',
 'Diagnostic des temps de réponse sur les transactions agences', 'Plaintes récurrentes aux heures de pointe', 'Mise en place de sondes APM et capture des métriques', NULL, NULL, 'Accès VPN confirmés.'),

('Interconnexion OTR Impôts', CONCAT('ORD-', UNIX_TIMESTAMP(), '-22'), 'CORIS', 'Jean-Luc Amegan', 'Mme Kaboré', 
 '2026-11-16 08:30:00', '2026-11-16 12:30:00', 0.5, 'Assistance', 'Planifiée', 'En ligne',
 'OP-1145', NULL, 0, 1, 
 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 'Opérations',
 'Contrôle des flux d avis de mise en recouvrement OTR', 'Assurer la fluidité des déclarations fiscales', 'Vérification des tables de correspondances de comptes trésor', NULL, NULL, 'Spécifications validées.'),

('Plan de Continuité PCA AMPLITUDE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-23'), 'ORABANK', 'Koffi Mensah', 'M. Ayivi', 
 '2026-11-18 10:00:00', '2026-11-18 18:00:00', 1.0, 'Maintenance', 'Planifiée', 'En présentiel',
 'OP-1150', NULL, 1, 0, 
 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 'Gestion des Risques',
 'Exercice semestriel de bascule sur site de secours', 'Exigence de conformité Banque Centrale', 'Simulation de panne totale du site principal et activation du site miroir', NULL, NULL, 'Notification préalable à la direction.'),

('MBANKING Notification Push', CONCAT('ORD-', UNIX_TIMESTAMP(), '-24'), 'SUNU', 'Ablam Lawson', 'M. Dossou', 
 '2026-11-20 09:00:00', '2026-11-20 16:00:00', 0.8, 'Programmation', 'Planifiée', 'En ligne',
 'OP-1155', NULL, 0, 1, 
 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 'Canaux Digitaux',
 'Intégration des notifications push Firebase pour l application MBANKING', 'Amélioration de la satisfaction client', 'Développement du service de push et tests sur terminaux Android/iOS', NULL, NULL, 'Cahier de charges validé.'),

('Supervision & Reporting SEGUCE', CONCAT('ORD-', UNIX_TIMESTAMP(), '-25'), 'UTB', 'Jean-Luc Amegan', 'M. Hounkpati', 
 '2026-11-22 09:00:00', '2026-11-22 17:00:00', 1.0, 'Formation', 'Planifiée', 'En ligne',
 'OP-1160', NULL, 0, 1, 
 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'Contrôle Interne',
 'Formation des auditeurs sur la réconciliation des paiements SEGUCE', 'Nouvelle équipe d audit', 'Présentation des rapports automatisés de rapprochement', NULL, NULL, 'Supports de cours préparés.');


-- =========================================================================================
-- 4. INSERTION DE 25 TICKETS D'INCIDENTS ET DEMANDES (Catégories réelles)
-- =========================================================================================

INSERT INTO `cntkt` (
    `tktnum`, `tkttit`, `tktapp`, `tkturg`, `tkttyp`, `tktsta`, 
    `tktdes`, `tktcau`, `tktsol`, `cliid`, `useid`, `misint`,
    `tktcdt`, `tktudt`, `tktrdt`
) VALUES
(CONCAT('INC-', UNIX_TIMESTAMP(), '-01'), 'Erreur 500 sur consultation de solde MBANKING', 'MBANKING', 'URGENTE', 'INCIDENT', 'CLOS',
 'Plusieurs clients signalent une page blanche lors de l affichage du solde sur l application mobile.', 'Fuite de connexion sur le pool JDBC Glassfish.', 'Redémarrage du pool de connexions et correction du leak dans le filtre d authentification.', @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-10-01 09:15:00', '2026-10-01 11:30:00', '2026-10-01 11:30:00'),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-02'), 'Création d un nouveau profil caissier principal sur AMPLITUDE', 'AMPLITUDE', 'NORMALE', 'DEMANDE', 'CLOS',
 'Merci de créer les droits d accès pour notre nouveau chef de caisse à l agence centrale.', NULL, 'Profil créé avec les autorisations de double signature validées.', @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-10-02 10:00:00', '2026-10-02 14:00:00', '2026-10-02 14:00:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-03'), 'Échec de transmission des quittances SEGUCE', 'SEGUCE', 'BLOQUANTE', 'INCIDENT', 'CLOS',
 'Les quittances de dédouanement portuaire ne sont plus validées depuis 08h.', 'Certificat de signature électronique expiré.', 'Régénération et déploiement immédiat du nouveau certificat.', @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-10-03 08:30:00', '2026-10-03 10:15:00', '2026-10-03 10:15:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-04'), 'Rejet des télépaiements d impôts sur le portail OTR', 'OTR', 'URGENTE', 'INCIDENT', 'CLOS',
 'Les déclarations fiscales renvoient une erreur code 404 vers le serveur de télécollecte.', 'Endpoint modifié suite à une maintenance sur le serveur OTR.', 'Mise à jour de l URL de callback et test avec 3 déclarations tests.', @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-10-04 14:00:00', '2026-10-04 16:30:00', '2026-10-04 16:30:00'),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-05'), 'Activation du module de paiement marchand sur EPAIEMENT', 'EPAIEMENT', 'NORMALE', 'DEMANDE', 'CLOS',
 'Un nouveau grand compte souhaite intégrer la passerelle de paiement en ligne sur son site e-commerce.', NULL, 'Clés API marchandes générées et transmises sous pli confidentiel.', @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-10-05 11:00:00', '2026-10-05 17:00:00', '2026-10-05 17:00:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-06'), 'Blocage lors de la clôture journalière AMPLITUDE', 'AMPLITUDE', 'BLOQUANTE', 'INCIDENT', 'CLOS',
 'Le traitement batch s arrête à l étape 4 (calcul des intérêts débiteurs).', 'Enregistrement corrompu avec un solde négatif anormal sur compte gelé.', 'Correction manuelle de la ligne en base et relance du batch.', @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-10-07 07:00:00', '2026-10-07 09:20:00', '2026-10-07 09:20:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-07'), 'Non-réception des codes OTP de validation MBANKING', 'MBANKING', 'NORMALE', 'INCIDENT', 'RESOLU',
 'Les clients ne reçoivent pas les SMS de validation des virements.', 'File d attente de la passerelle SMS surchargée.', 'Purge de la file et redémarrage du service d envoi.', @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-10-09 10:30:00', '2026-10-09 18:00:00', '2026-10-09 18:00:00'),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-08'), 'Paramétrage d un nouveau palier de commission OTR', 'OTR', 'FAIBLE', 'DEMANDE', 'RESOLU',
 'Nouvelle directive fiscale fixant les frais à 0.5% pour les paiements d impôts fonciers.', NULL, 'Table des barèmes OTR mise à jour et validée en recette.', @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-10-11 09:00:00', '2026-10-12 15:00:00', '2026-10-12 15:00:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-09'), 'Désynchronisation des écritures SEGUCE vers AMPLITUDE', 'SEGUCE', 'URGENTE', 'INCIDENT', 'RESOLU',
 'Les quittances payées sur SEGUCE n alimentent pas automatiquement le compte général 512.', 'Job d intégration nocturne désactivé par mégarde.', 'Réactivation du cron et exécution manuelle pour rattraper les écritures.', @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-10-13 13:45:00', '2026-10-13 15:30:00', '2026-10-13 15:30:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-10'), 'Échec des remboursements en ligne sur EPAIEMENT', 'EPAIEMENT', 'URGENTE', 'INCIDENT', 'RESOLU',
 'Impossible d effectuer un refund partiel depuis le back-office commerçant.', 'Bug dans le calcul de la TVA sur commission remboursée.', 'Patch déployé sur la passerelle EPAIEMENT v2.2.1.', @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-10-15 16:00:00', '2026-10-15 19:45:00', '2026-10-15 19:45:00'),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-11'), 'Demande d extraction du journal des transactions EPAIEMENT', 'EPAIEMENT', 'FAIBLE', 'DEMANDE', 'RESOLU',
 'Le contrôle interne demande l historique des paiements carte du mois d octobre.', NULL, 'Fichier CSV chiffré transmis au responsable de l audit.', @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-10-17 11:20:00', '2026-10-18 10:00:00', '2026-10-18 10:00:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-12'), 'Crash de l application mobile MBANKING sur Android 14', 'MBANKING', 'NORMALE', 'INCIDENT', 'RESOLU',
 'L application se ferme inopinément à l ouverture sur les versions récentes Android.', 'Incompatibilité avec le nouveau gestionnaire de permissions système.', 'Mise à jour du SDK mobile et publication du correctif sur le Play Store.', @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-10-19 14:10:00', '2026-10-19 17:00:00', '2026-10-19 17:00:00'),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-13'), 'Ajout de la quittance OTR téléchargeable en format PDF', 'OTR', 'NORMALE', 'DEMANDE', 'RESOLU',
 'Les contribuables souhaitent télécharger directement leur reçu fiscal en PDF avec code QR.', NULL, 'Générateur de PDF iText configuré avec signature électronique.', @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-10-21 09:30:00', '2026-10-22 16:00:00', '2026-10-22 16:00:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-14'), 'Impossibilité d éditer les fiches de compte sur AMPLITUDE', 'AMPLITUDE', 'NORMALE', 'INCIDENT', 'RESOLU',
 'Erreur d impression signalée dans 4 agences réseau.', 'Police de caractères manquante sur le serveur d impression Ghostscript.', 'Installation des polices manquantes et redémarrage du spouleur.', @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-10-23 15:00:00', '2026-10-24 11:30:00', '2026-10-24 11:30:00'),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-15'), 'Interruption du service SEGUCE pour les déclarations maritimes', 'SEGUCE', 'BLOQUANTE', 'INCIDENT', 'EN_COURS',
 'Aucun manifeste douanier ne peut être transmis depuis ce matin.', 'Liaison fibre optique principale coupée entre le data center et le port.', NULL, @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-11-01 08:00:00', '2026-11-01 09:15:00', NULL),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-16'), 'Intégration d Apple Pay sur la passerelle EPAIEMENT', 'EPAIEMENT', 'URGENTE', 'DEMANDE', 'EN_COURS',
 'Demande de la direction générale pour les paiements en ligne sans contact.', NULL, NULL, @default_client_id, @default_user_id, 'Ablam Lawson',
 '2026-11-02 10:00:00', '2026-11-02 14:00:00', NULL),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-17'), 'Différence de solde entre MBANKING et AMPLITUDE', 'MBANKING', 'URGENTE', 'INCIDENT', 'EN_COURS',
 'Le solde affiché sur mobile ne prend pas en compte les opérations de la veille.', 'Retard d indexation sur l API de synchronisation des comptes.', NULL, @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-11-03 17:30:00', '2026-11-04 08:30:00', NULL),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-18'), 'Raccordement d un nouveau bureau de douane sur SEGUCE', 'SEGUCE', 'NORMALE', 'DEMANDE', 'EN_COURS',
 'Ouverture du poste frontalier de Cinkassé et raccordement au système SEGUCE.', NULL, NULL, @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-11-04 09:00:00', '2026-11-04 11:00:00', NULL),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-19'), 'Lenteur lors de la génération des bordereaux OTR', 'OTR', 'NORMALE', 'INCIDENT', 'EN_ATTENTE',
 'Temps d attente supérieur à 2 minutes pour obtenir le bordereau de paiement des taxes.', 'Optimisation de la requête de jointure SQL en attente de validation.', NULL, @default_client_id, @default_user_id, 'Koffi Mensah',
 '2026-11-04 08:15:00', '2026-11-04 10:00:00', NULL),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-20'), 'Activation de la biométrie (FaceID) sur MBANKING', 'MBANKING', 'NORMALE', 'DEMANDE', 'EN_ATTENTE',
 'Permettre l authentification rapide par reconnaissance faciale sur iPhone.', NULL, NULL, @default_client_id, @default_user_id, 'Jean-Luc Amegan',
 '2026-11-05 14:00:00', '2026-11-05 15:30:00', NULL),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-21'), 'Rejet de paiement marchand avec code erreur 96 sur EPAIEMENT', 'EPAIEMENT', 'BLOQUANTE', 'INCIDENT', 'NOUVEAU',
 'Les clients du site e-commerce ne peuvent plus payer par carte Visa.', NULL, NULL, @default_client_id, @default_user_id, NULL,
 '2026-11-06 08:30:00', '2026-11-06 08:30:00', NULL),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-22'), 'Création d un nouveau compte administrateur sur OTR Passerelle', 'OTR', 'NORMALE', 'DEMANDE', 'NOUVEAU',
 'Demande pour le nouvel inspecteur des impôts assigné à la division bancaire.', NULL, NULL, @default_client_id, @default_user_id, NULL,
 '2026-11-06 09:15:00', '2026-11-06 09:15:00', NULL),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-23'), 'Blocage de l écran d édition des attestations sous AMPLITUDE', 'AMPLITUDE', 'FAIBLE', 'INCIDENT', 'NOUVEAU',
 'L attestation de clôture de compte génère une erreur interne Java.', NULL, NULL, @default_client_id, @default_user_id, NULL,
 '2026-11-06 10:00:00', '2026-11-06 10:00:00', NULL),

(CONCAT('DEM-', UNIX_TIMESTAMP(), '-24'), 'Ajout de la notification WhatsApp pour les transactions SEGUCE', 'SEGUCE', 'NORMALE', 'DEMANDE', 'NOUVEAU',
 'Permettre aux transitaires de recevoir leur numéro de quittance par WhatsApp.', NULL, NULL, @default_client_id, @default_user_id, NULL,
 '2026-11-06 10:45:00', '2026-11-06 10:45:00', NULL),

(CONCAT('INC-', UNIX_TIMESTAMP(), '-25'), 'Problème de synchronisation des virements entre MBANKING et AMPLITUDE', 'MBANKING', 'URGENTE', 'INCIDENT', 'NOUVEAU',
 'Les virements initiés depuis l application mobile restent en attente de débit comptable.', NULL, NULL, @default_client_id, @default_user_id, NULL,
 '2026-11-06 11:30:00', '2026-11-06 11:30:00', NULL);
