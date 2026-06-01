package com.ace3i.connect.service;

public interface IDatabaseService {
    /**
     * Génère un script SQL contenant les instructions INSERT pour toutes les tables.
     * @return Le script SQL sous forme de chaîne de caractères.
     */
    String exportDatabase();

    /**
     * Exécute les instructions SQL fournies pour restaurer la base de données.
     * @param sqlData Les données SQL à importer.
     */
    void importDatabase(byte[] sqlData);

    /**
     * Vide toutes les tables de la base de données (sauf les comptes admin pour éviter le blocage).
     */
    void clearDatabase();
}
