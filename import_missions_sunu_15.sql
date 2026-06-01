-- Script d'insertion de 15 missions pour SUNU (Janvier à Mars 2026)
-- Basé sur la structure de la table CNMIS

INSERT INTO CNMIS (MISPRJ, MISCLI, MISINT, MISICL, MISDEB, MISFIN, MISORD, MISOPR, MISSTA, MISOTN, MISOTL, MISDES, MISOCL, MISOIN, MISPCH, MISPCO, MISTAN, MISTFO, MISTMA, MISTPR, MISTAS, MISTIN, MISTME, MISTSU) VALUES

('Audit de Conformité RGPD', 'SUNU', 'Alice', 'M. Koffi', '2026-01-05 08:30:00', '2026-01-09 17:30:00', 'OM-2026-101', 'En présentiel', 'Terminée', NULL, NULL, 
'Analyse des processus de traitement des données personnelles au sein de la direction Vie. La mission a couvert la revue des registres de traitement, l''analyse des durées de conservation des données clients et la vérification des mentions d''information sur les formulaires de souscription. Nous avons également audité les clauses contractuelles avec les sous-traitants informatiques pour garantir la conformité au cadre réglementaire.', 
'Le rapport d''audit est très instructif et met en lumière des points de vigilance sur nos anciennes archives papier. Les recommandations pour la mise en place d''un DPO externalisé sont à l''étude. Nous apprécions la pédagogie de l''intervenante.', 
'Une bonne implication des équipes métiers, mais un manque de centralisation des documents juridiques a été noté. Il est impératif de nommer un référent par département pour assurer le suivi des actions de remédiation préconisées.', 
true, false, true, false, false, false, false, false, false, true),

('Optimisation Architecture LAN', 'SUNU', 'Jean-Luc', 'M. Bakayoko', '2026-01-12 09:00:00', '2026-01-15 18:00:00', 'OM-2026-102', 'En présentiel', 'Terminée', NULL, NULL, 
'Segmentation du réseau local (VLAN) pour isoler les flux de la téléphonie IP des flux de données bureautiques. La mission a inclus la reconfiguration des switches de cœur de réseau, la mise en place de listes de contrôle d''accès (ACL) et l''optimisation du protocole Spanning-Tree pour éviter les boucles réseau. Des tests de performance ont validé une réduction de la latence sur les applications métiers critiques.', 
'Nous constatons une nette amélioration de la stabilité du réseau depuis l''intervention. Les problèmes de micro-coupures lors des appels VoIP ont disparu. L''expert a su travailler sans impacter la production durant les heures de bureau.', 
'L''infrastructure réseau est performante mais certains câbles de brassage en catégorie 5e devraient être remplacés par du Cat6 pour supporter les futurs débits. La documentation des VLANs a été mise à jour et remise au responsable IT.', 
false, true, false, false, true, false, false, true, true, false),

('Mise en place Proxy Sécurisé', 'SUNU', 'Marc', 'Mme Sidibé', '2026-01-20 08:00:00', '2026-01-23 17:00:00', 'OM-2026-103', 'En ligne', 'Terminée', NULL, NULL, 
'Installation et configuration d''une solution de filtrage web (Proxy) pour sécuriser la navigation des collaborateurs. Mise en place de politiques de filtrage par catégories (réseaux sociaux, streaming, sites malveillants), activation de l''inspection SSL et couplage avec l''Active Directory pour un reporting par utilisateur. Nous avons également configuré des alertes en temps réel en cas de détection de téléchargement suspect.', 
'La solution répond à nos besoins de sécurité et de conformité. Le portail d''alerte est clair pour les utilisateurs. Nous apprécions la réactivité de l''expert lors de la phase de réglage des faux positifs sur certains sites institutionnels.', 
'Certains navigateurs obsolètes ont nécessité une installation manuelle du certificat de confiance du Proxy. Il est conseillé d''automatiser la mise à jour des navigateurs via GPO pour garantir l''efficacité du filtrage applicatif.', 
false, true, false, false, true, false, true, true, false, false),

('Révision Plan de Sauvegarde', 'SUNU', 'Alice', 'M. Bamba', '2026-01-28 09:00:00', '2026-01-30 16:00:00', 'OM-2026-104', 'En présentiel', 'Terminée', NULL, NULL, 
'Audit et optimisation de la stratégie de sauvegarde actuelle. La mission a consisté à vérifier l''intégrité des sauvegardes sur bandes, à configurer de nouveaux jobs pour les bases de données Oracle récemment déployées, et à mettre en place une réplication quotidienne vers un site distant. Un test de restauration granulaire d''une boîte aux lettres Exchange a été réalisé avec succès pour valider la procédure.', 
'Nous sommes rassurés par les résultats des tests de restauration. La nouvelle politique de rétention est plus adaptée à nos obligations légales. L''intervenante a fait preuve d''un grand professionnalisme et d''une expertise pointue.', 
'Le lecteur de bandes actuel arrive en fin de vie et présente des signes de fatigue mécanique. Un remplacement par une librairie LTO-8 est vivement recommandé avant la fin du trimestre pour éviter tout risque d''échec matériel.', 
false, true, false, false, false, false, true, true, false, false),

('Sécurisation Postes Nomades', 'SUNU', 'Stani', 'M. Fall', '2026-02-02 08:30:00', '2026-02-06 17:30:00', 'OM-2026-105', 'En ligne', 'Terminée', NULL, NULL, 
'Déploiement d''un client VPN Always-On et chiffrement des disques durs (BitLocker) sur 50 ordinateurs portables destinés à la force commerciale. La mission a inclus la configuration de l''authentification multi-facteurs (MFA) via Azure AD, la mise en place de politiques de sécurité strictes sur les ports USB et la formation des utilisateurs aux bonnes pratiques en situation de mobilité.', 
'Les commerciaux peuvent désormais travailler à distance en toute sécurité. L''authentification par code sur mobile est simple et bien acceptée. Le support fourni durant le déploiement a été excellent et très réactif.', 
'Quelques difficultés initiales ont été rencontrées avec les cartes SIM 4G de certains opérateurs qui bloquaient le trafic VPN. Une modification des paramètres APN a résolu le problème. Tous les postes sont désormais chiffrés et suivis.', 
true, true, false, false, true, false, true, true, false, false),

('Audit de Performance ERP', 'SUNU', 'Marc', 'Mme Compaoré', '2026-02-10 09:00:00', '2026-02-13 18:00:00', 'OM-2026-106', 'En ligne', 'Terminée', NULL, NULL, 
'Analyse des lenteurs signalées par les utilisateurs sur le module de gestion des sinistres. La mission a porté sur le profilage des requêtes SQL les plus coûteuses, l''analyse de la consommation mémoire du serveur d''application Tomcat et la vérification des paramètres du ramasse-miettes (Garbage Collector) Java. Des optimisations d''indexation ont été proposées pour réduire les temps de réponse.', 
'Nous avons observé un gain de fluidité immédiat après l''application des recommandations sur la base de données. Les rapports hebdomadaires se génèrent deux fois plus vite. L''expertise technique de Marc a été déterminante.', 
'Le serveur d''application manque de ressources CPU lors des pics d''activité de fin de mois. Une migration vers une instance avec plus de coeurs virtuels est préconisée pour absorber la charge de travail croissante des gestionnaires.', 
false, true, false, false, true, true, false, false, false, false),

('Installation Solution Antivirus EDR', 'SUNU', 'Alice', 'M. Kambou', '2026-02-16 08:00:00', '2026-02-20 18:00:00', 'OM-2026-107', 'En présentiel', 'Terminée', NULL, NULL, 
'Remplacement de l''ancien antivirus par une solution EDR (Endpoint Detection and Response) de nouvelle génération. Déploiement sur 200 terminaux, configuration de la console d''administration centralisée et mise en place de scénarios de réponse automatique aux incidents (isolation des postes infectés). Une session de transfert de compétences a été réalisée avec l''équipe de sécurité interne.', 
'La visibilité sur les menaces est bien plus grande qu''auparavant. Nous apprécions la capacité de la solution à détecter les comportements anormaux même sans signature connue. L''accompagnement d''Alice a été parfait de bout en bout.', 
'Le déploiement s''est fait sans heurts via les outils de gestion de parc. Quelques faux positifs sur des scripts administratifs internes ont été rapidement corrigés par la création d''exclusions spécifiques. La console est désormais stable.', 
false, true, false, false, true, false, true, true, false, false),

('Migration de Serveur de Fichiers', 'SUNU', 'Jean-Luc', 'M. Koffi', '2026-02-24 09:00:00', '2026-02-27 17:00:00', 'OM-2026-108', 'En présentiel', 'Terminée', NULL, NULL, 
'Migration des données d''un ancien serveur Windows 2012 vers une nouvelle infrastructure sous Windows 2022. La mission a inclus la reprise des droits d''accès (ACL) NTFS, la mise en place de quotas de stockage par département et l''activation de la déduplication de données pour optimiser l''espace disque. Nous avons également configuré des clichés instantanés (Shadow Copies) pour permettre aux utilisateurs de restaurer leurs fichiers.', 
'La migration a été transparente pour les utilisateurs. Les nouveaux quotas permettent une meilleure maîtrise de la croissance des données. Le travail de Jean-Luc est propre et méticuleux, comme à son habitude.', 
'Le volume de données "froides" (non consultées depuis plus de 2 ans) représente près de 40% de l''espace total. Un projet d''archivage long terme devrait être envisagé pour libérer de l''espace sur le stockage performant.', 
false, true, false, false, true, false, false, true, false, false),

('Formation Cybersécurité Staff', 'SUNU', 'Stani', 'M. Bakayoko', '2026-03-02 09:00:00', '2026-03-04 16:00:00', 'OM-2026-109', 'En présentiel', 'Terminée', NULL, NULL, 
'Animation de sessions de sensibilisation à la cybersécurité pour le personnel administratif et de direction. Les thèmes abordés comprenaient la reconnaissance du phishing, la gestion des mots de passe, la sécurité sur les réseaux sociaux et les risques liés à l''ingénierie sociale. Des exercices pratiques de détection d''emails frauduleux ont été réalisés avec les participants.', 
'Les retours des collaborateurs sont très positifs, les exemples concrets ont beaucoup marqué les esprits. Nous pensons que cela va réduire significativement notre exposition aux risques humains. Stani est un excellent communicant.', 
'Le niveau de connaissance initial était assez hétérogène. L''utilisation d''une plateforme de e-learning en complément de ces sessions présentielles permettrait de maintenir un niveau de vigilance élevé tout au long de l''année.', 
false, false, true, true, false, false, false, false, false, false),

('Audit WiFi et Couverture Radio', 'SUNU', 'Marc', 'Mme Sidibé', '2026-03-09 08:30:00', '2026-03-11 17:30:00', 'OM-2026-110', 'En présentiel', 'Terminée', NULL, NULL, 
'Réalisation d''un "Site Survey" complet pour optimiser la couverture WiFi du siège. Analyse des interférences radio, vérification de la configuration des bornes existantes et identification des zones d''ombre. Nous avons proposé un nouveau plan de positionnement des points d''accès pour garantir un roaming fluide entre les étages et une bande passante stable pour les visioconférences.', 
'L''audit confirme nos problèmes de connexion dans les salles de réunion du 3ème étage. Le plan d''action proposé est clair et nous allons procéder aux ajustements recommandés rapidement. Très bon rapport technique.', 
'La présence de nombreuses cloisons métalliques dans certaines zones dégrade fortement le signal. L''ajout de trois bornes supplémentaires et le passage sur la bande des 5GHz pour les terminaux compatibles sont nécessaires.', 
false, true, false, false, false, false, false, true, true, false),

('Mise à jour vSphere 7.0', 'SUNU', 'Alice', 'M. Bamba', '2026-03-16 09:00:00', '2026-03-19 18:00:00', 'OM-2026-111', 'En présentiel', 'Terminée', NULL, NULL, 
'Migration de l''infrastructure de virtualisation VMware vSphere de la version 6.5 vers la version 7.0. La mission a couvert la mise à jour du vCenter Server, des hôtes ESXi et des VMware Tools sur les machines virtuelles. Nous avons également optimisé la configuration de la haute disponibilité (HA) et du répartiteur de charge de ressources (DRS) pour améliorer la résilience du cluster.', 
'La montée en version s''est déroulée sans interruption de service grâce à une planification rigoureuse. Nous disposons maintenant de nouvelles fonctionnalités de sécurité et d''une interface d''administration plus moderne.', 
'Deux anciens serveurs ne sont plus compatibles avec la version 7.0 et devront rester isolés jusqu''à leur remplacement prochain. L''intervenante a su gérer cette contrainte technique avec brio sans impacter le reste du projet.', 
false, true, false, false, true, false, false, false, true, false),

('Configuration Monitoring Zabbix', 'SUNU', 'Jean-Luc', 'M. Fall', '2026-03-23 08:00:00', '2026-03-26 18:00:00', 'OM-2026-112', 'En ligne', 'Terminée', NULL, NULL, 
'Extension du système de supervision Zabbix pour inclure le monitoring des onduleurs, des switchs de distribution et des bases de données de production. Mise en place de tableaux de bord personnalisés pour l''équipe d''exploitation et configuration des alertes SMS pour les incidents critiques. Nous avons également intégré des tests de scénarios web pour surveiller la disponibilité du portail client.', 
'Nous avons enfin une vision temps réel de l''état de santé de nos équipements critiques. Les alertes nous permettent d''intervenir avant que les utilisateurs ne ressentent un impact. Jean-Luc a fait un excellent travail d''intégration.', 
'Le serveur de monitoring actuel commence à saturer au niveau de l''écriture en base de données (IOPS). Une optimisation du "Housekeeper" Zabbix et le passage de la DB sur un disque SSD dédié sont recommandés.', 
false, true, false, false, true, false, false, true, false, false),

('Assistance Audit Interne IT', 'SUNU', 'Stani', 'M. Kambou', '2026-03-27 09:00:00', '2026-03-28 17:00:00', 'OM-2026-113', 'En présentiel', 'Terminée', NULL, NULL, 
'Accompagnement de l''auditeur interne pour la revue des contrôles généraux informatiques (ITGC). Fourniture des preuves concernant la gestion des habilitations, le suivi des changements en production et la revue des logs d''accès aux données sensibles. Nous avons également aidé à la rédaction des réponses techniques aux premières constatations de l''auditeur.', 
'L''appui de Stani a été précieux pour traduire les exigences de l''auditeur en preuves techniques concrètes. Cela nous a permis de gagner un temps considérable et de démontrer la maturité de nos processus.', 
'Certains processus de validation des accès restent manuels et difficiles à tracer sur de longues périodes. L''implémentation d''un outil de "Workflow" pour les demandes d''habilitations est un axe d''amélioration identifié.', 
true, false, true, false, false, false, false, false, false, true),

('Refonte Infrastructure Stockage', 'SUNU', 'Marc', 'Mme Compaoré', '2026-01-15 09:00:00', '2026-01-20 18:00:00', 'OM-2026-114', 'En présentiel', 'Terminée', NULL, NULL, 
'Installation d''une nouvelle baie de stockage All-Flash pour héberger les bases de données de production. La mission a inclus le zoning Fibre Channel, la création des volumes et la migration à chaud des machines virtuelles les plus exigeantes. Nous avons également mis en place une réplication synchrone pour garantir une perte de donnée nulle (RPO=0) en cas d''incident majeur sur la baie principale.', 
'Les performances sont impressionnantes, les traitements batch de nuit ont été réduits de moitié. La baie est simple à administrer et offre une visibilité très fine sur les consommations de ressources.', 
'La configuration du Multipathing sur certains anciens serveurs Linux a nécessité une attention particulière pour éviter des déconnexions lors des tests de basculement. Tout est désormais parfaitement fonctionnel et documenté.', 
false, true, false, false, true, false, false, true, false, false),

('Installation Système Visio Rooms', 'SUNU', 'Stani', 'M. Dramé', '2026-02-15 08:00:00', '2026-02-18 18:00:00', 'OM-2026-115', 'En présentiel', 'Terminée', NULL, NULL, 
'Équipement de deux grandes salles de conférence avec des solutions de visioconférence intégrées Microsoft Teams Rooms. Installation des barres de son, des caméras 4K avec suivi automatique et des tablettes de contrôle tactile. La mission a inclus la configuration réseau pour garantir la qualité de service (QoS) et la formation des assistantes de direction à l''utilisation du matériel.', 
'Les réunions avec nos filiales régionales sont désormais beaucoup plus fluides et professionnelles. Le matériel est intuitif et la qualité audio est excellente. Stani a réalisé une installation très propre et soignée.', 
'L''éclairage de l''une des salles provoquait des reflets sur l''écran principal. Un léger repositionnement du support mural a permis de corriger le problème. Une documentation simplifiée a été plastifiée et laissée dans chaque salle.', 
false, true, false, false, false, false, false, true, false, false);
