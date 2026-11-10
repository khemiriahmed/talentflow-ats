package com.talentflow.storage;

/**
 * Abstraction du stockage de fichiers (CV).
 * Implémentation S3 : fonctionne avec AWS S3, un mock S3 local, ou GCS en mode interopérabilité.
 * Les fichiers sont limités à 5 Mo : on les manipule en mémoire (byte[]) pour rester simple.
 */
public interface StorageService {

    void upload(String key, byte[] content, String contentType);

    byte[] download(String key);

    void delete(String key);
}
