package com.prdv.rdv.profile.application.service.support;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Restitution des fichiers multimedias (photos, scans, video).
 *
 * <p>Le domaine ne conserve pas le type MIME d'origine de ces fichiers : le
 * type et le nom affiches au telechargement sont donc deduits de la cle de
 * stockage, de la forme {@code dossier/UUID-nom.extension}.
 */
public final class MediaContentTypes {

    public static final String OCTET_STREAM = "application/octet-stream";

    private static final Pattern UUID_PREFIX = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-(.+)$");

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("heic", "image/heic"),
            Map.entry("tif", "image/tiff"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("dcm", "application/dicom"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"));

    private MediaContentTypes() {
    }

    /** Type MIME deduit de l'extension du fichier stocke (repli : octet-stream). */
    public static String contentTypeOf(String storageKey) {
        String name = filenameOf(storageKey);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return OCTET_STREAM;
        }
        return BY_EXTENSION.getOrDefault(name.substring(dot + 1).toLowerCase(Locale.ROOT), OCTET_STREAM);
    }

    /** Nom de fichier d'origine : la cle sans son dossier ni son prefixe UUID. */
    public static String filenameOf(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return "media";
        }
        String last = storageKey.substring(storageKey.lastIndexOf('/') + 1);
        Matcher matcher = UUID_PREFIX.matcher(last);
        return matcher.matches() ? matcher.group(1) : last;
    }
}
