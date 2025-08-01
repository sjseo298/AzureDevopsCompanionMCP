package com.mcp.server.utils.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad centralizada para realizar backup de archivos de configuración.
 * Proporciona funcionalidad para respaldar archivos antes de modificarlos.
 */
public class ConfigurationBackupUtil {
    
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String BACKUP_SUFFIX = ".backup";
    
    /**
     * Realiza backup de un archivo individual.
     * 
     * @param filePath ruta del archivo a respaldar
     * @return información del backup realizado
     */
    public static BackupResult backupFile(String filePath) {
        return backupFile(Paths.get(filePath));
    }
    
    /**
     * Realiza backup de un archivo individual.
     * 
     * @param filePath Path del archivo a respaldar
     * @return información del backup realizado
     */
    public static BackupResult backupFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return new BackupResult(filePath.toString(), false, "Archivo no existe", null);
            }
            
            String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
            String backupFileName = filePath.getFileName().toString() + BACKUP_SUFFIX + "_" + timestamp;
            Path backupPath = filePath.getParent().resolve(backupFileName);
            
            Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            return new BackupResult(filePath.toString(), true, "Backup exitoso", backupPath.toString());
            
        } catch (IOException e) {
            return new BackupResult(filePath.toString(), false, "Error de E/S: " + e.getMessage(), null);
        } catch (Exception e) {
            return new BackupResult(filePath.toString(), false, "Error inesperado: " + e.getMessage(), null);
        }
    }
    
    /**
     * Realiza backup de múltiples archivos.
     * 
     * @param filePaths lista de rutas de archivos a respaldar
     * @return lista de resultados de backup
     */
    public static List<BackupResult> backupFiles(List<String> filePaths) {
        List<BackupResult> results = new ArrayList<>();
        
        for (String filePath : filePaths) {
            results.add(backupFile(filePath));
        }
        
        return results;
    }
    
    /**
     * Realiza backup de archivos de configuración estándar.
     * 
     * @return lista de resultados de backup
     */
    public static List<BackupResult> backupStandardConfigFiles() {
        List<String> standardConfigFiles = List.of(
            "config/organization-config.yml",
            "config/discovered-organization.yml",
            "config/field-mappings.yml",
            "config/business-rules.yml"
        );
        
        return backupFiles(standardConfigFiles);
    }
    
    /**
     * Genera un reporte de backup en formato texto.
     * 
     * @param backupResults lista de resultados de backup
     * @return reporte formateado
     */
    public static String generateBackupReport(List<BackupResult> backupResults) {
        StringBuilder report = new StringBuilder();
        report.append("💾 **Reporte de Backup de Configuración**\n");
        report.append("========================================\n");
        report.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        int successful = 0;
        int failed = 0;
        
        for (BackupResult result : backupResults) {
            if (result.isSuccessful()) {
                successful++;
                report.append("✅ **").append(getFileName(result.getOriginalPath())).append("**\n");
                report.append("   Respaldado como: ").append(getFileName(result.getBackupPath())).append("\n");
            } else {
                failed++;
                report.append("❌ **").append(getFileName(result.getOriginalPath())).append("**\n");
                report.append("   Error: ").append(result.getErrorMessage()).append("\n");
            }
            report.append("\n");
        }
        
        report.append("**Resumen:**\n");
        report.append("- Archivos respaldados exitosamente: ").append(successful).append("\n");
        report.append("- Archivos con error: ").append(failed).append("\n");
        report.append("- Total procesados: ").append(backupResults.size()).append("\n");
        
        return report.toString();
    }
    
    /**
     * Verifica si existe un backup de un archivo.
     * 
     * @param originalFilePath ruta del archivo original
     * @return true si existe al menos un backup
     */
    public static boolean hasBackup(String originalFilePath) {
        try {
            Path filePath = Paths.get(originalFilePath);
            Path parentDir = filePath.getParent();
            
            if (parentDir == null || !Files.exists(parentDir)) {
                return false;
            }
            
            String fileName = filePath.getFileName().toString();
            String backupPrefix = fileName + BACKUP_SUFFIX;
            
            return Files.list(parentDir)
                    .anyMatch(path -> path.getFileName().toString().startsWith(backupPrefix));
                    
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Lista todos los backups disponibles para un archivo.
     * 
     * @param originalFilePath ruta del archivo original
     * @return lista de rutas de backup
     */
    public static List<String> listBackups(String originalFilePath) {
        List<String> backups = new ArrayList<>();
        
        try {
            Path filePath = Paths.get(originalFilePath);
            Path parentDir = filePath.getParent();
            
            if (parentDir == null || !Files.exists(parentDir)) {
                return backups;
            }
            
            String fileName = filePath.getFileName().toString();
            String backupPrefix = fileName + BACKUP_SUFFIX;
            
            Files.list(parentDir)
                    .filter(path -> path.getFileName().toString().startsWith(backupPrefix))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString())) // Más reciente primero
                    .forEach(path -> backups.add(path.toString()));
                    
        } catch (Exception e) {
            // Lista vacía en caso de error
        }
        
        return backups;
    }
    
    /**
     * Restaura un archivo desde su backup más reciente.
     * 
     * @param originalFilePath ruta del archivo a restaurar
     * @return resultado de la operación de restauración
     */
    public static RestoreResult restoreFromBackup(String originalFilePath) {
        List<String> backups = listBackups(originalFilePath);
        
        if (backups.isEmpty()) {
            return new RestoreResult(originalFilePath, false, "No hay backups disponibles", null);
        }
        
        String mostRecentBackup = backups.get(0);
        
        try {
            Path originalPath = Paths.get(originalFilePath);
            Path backupPath = Paths.get(mostRecentBackup);
            
            Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            
            return new RestoreResult(originalFilePath, true, "Restauración exitosa", mostRecentBackup);
            
        } catch (IOException e) {
            return new RestoreResult(originalFilePath, false, "Error de E/S: " + e.getMessage(), mostRecentBackup);
        } catch (Exception e) {
            return new RestoreResult(originalFilePath, false, "Error inesperado: " + e.getMessage(), mostRecentBackup);
        }
    }
    
    /**
     * Extrae solo el nombre del archivo de una ruta completa.
     * 
     * @param fullPath ruta completa
     * @return nombre del archivo
     */
    private static String getFileName(String fullPath) {
        if (fullPath == null) return "N/A";
        return Paths.get(fullPath).getFileName().toString();
    }
    
    /**
     * Clase para representar el resultado de una operación de backup.
     */
    public static class BackupResult {
        private final String originalPath;
        private final boolean successful;
        private final String errorMessage;
        private final String backupPath;
        
        public BackupResult(String originalPath, boolean successful, String errorMessage, String backupPath) {
            this.originalPath = originalPath;
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.backupPath = backupPath;
        }
        
        public String getOriginalPath() { return originalPath; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
        public String getBackupPath() { return backupPath; }
    }
    
    /**
     * Clase para representar el resultado de una operación de restauración.
     */
    public static class RestoreResult {
        private final String originalPath;
        private final boolean successful;
        private final String errorMessage;
        private final String usedBackupPath;
        
        public RestoreResult(String originalPath, boolean successful, String errorMessage, String usedBackupPath) {
            this.originalPath = originalPath;
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.usedBackupPath = usedBackupPath;
        }
        
        public String getOriginalPath() { return originalPath; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
        public String getUsedBackupPath() { return usedBackupPath; }
    }
}
