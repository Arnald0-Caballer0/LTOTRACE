package com.arnaldo.tesis;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        // 1. Creamos la instancia de Tesseract
        ITesseract instance = new Tesseract();

        // 2. IMPORTANTE: Apuntamos a la carpeta 'tessdata' dentro de tu instalación
        // Usamos doble barra \\ para que Windows no se confunda
        instance.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");

        // 3. Configuramos el idioma (spa = español)
        instance.setLanguage("spa");

        try {
            // 4. Intentamos leer una imagen (Crea una imagen llamada 'prueba.png' en la carpeta raíz)
            File imageFile = new File("prueba.png");
            String result = instance.doOCR(imageFile);

            System.out.println("--- TEXTO EXTRAÍDO ---");
            System.out.println(result);
            System.out.println("-----------------------");

        } catch (TesseractException e) {
            System.err.println("Error al procesar el OCR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Asegúrate de que el archivo 'prueba.png' exista.");
        }
    }
}