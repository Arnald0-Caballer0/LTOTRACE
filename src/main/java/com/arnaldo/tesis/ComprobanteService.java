package com.arnaldo.tesis;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera los comprobantes en PDF del lado servidor (OpenPDF).
 * Por ahora: el Acta de Retiro de medios LTO.
 */
@Service
public class ComprobanteService {

    public byte[] generarActaRetiro(Cliente cliente, List<Cinta> cintas, String usuario) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC);

            // --- Título ---
            Paragraph titulo = new Paragraph("ACTA DE RETIRO DE MEDIOS LTO", fTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            doc.add(titulo);

            // --- Datos ---
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String nombreCli = (cliente != null && cliente.getNombre() != null) ? cliente.getNombre() : "-";
            String encargado = (cliente != null && cliente.getEncargado() != null) ? cliente.getEncargado() : "-";

            doc.add(parLabel("Empresa: ", nombreCli, fSub, fNormal));
            doc.add(parLabel("Encargado: ", encargado, fSub, fNormal));
            doc.add(parLabel("Fecha: ", LocalDate.now().format(fmt), fSub, fNormal));
            doc.add(parLabel("Cantidad de cintas: ", String.valueOf(cintas.size()), fSub, fNormal));

            Paragraph esp = new Paragraph(" ");
            esp.setSpacingAfter(10);
            doc.add(esp);

            // --- Tabla de cintas ---
            PdfPTable tabla = new PdfPTable(new float[]{1.5f, 2f, 2f});
            tabla.setWidthPercentage(100);
            tabla.addCell(celdaHead("Barcode", fSub));
            tabla.addCell(celdaHead("Volume ID", fSub));
            tabla.addCell(celdaHead("Tecnología", fSub));
            for (Cinta c : cintas) {
                tabla.addCell(celda(c.getBarcode(), fNormal));
                tabla.addCell(celda(c.getVolumeId(), fNormal));
                tabla.addCell(celda(c.getTecnologia(), fNormal));
            }
            doc.add(tabla);

            // --- Firma ---
            Paragraph firmaEsp = new Paragraph(" ");
            firmaEsp.setSpacingBefore(60);
            doc.add(firmaEsp);
            Paragraph linea = new Paragraph("____________________________");
            linea.setAlignment(Element.ALIGN_CENTER);
            doc.add(linea);
            Paragraph firma = new Paragraph("Firma de Recepción", fSub);
            firma.setAlignment(Element.ALIGN_CENTER);
            doc.add(firma);
            Paragraph firmaNom = new Paragraph(encargado, fNormal);
            firmaNom.setAlignment(Element.ALIGN_CENTER);
            doc.add(firmaNom);

            // --- Pie ---
            Paragraph pie = new Paragraph("Generado por LTOTRACE — Usuario: " + usuario, fSmall);
            pie.setAlignment(Element.ALIGN_RIGHT);
            pie.setSpacingBefore(30);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            System.err.println("Error al generar acta PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    // --- Helpers ---
    private Paragraph parLabel(String label, String valor, Font fLabel, Font fValor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, fLabel));
        p.add(new Chunk(valor, fValor));
        p.setSpacingAfter(4);
        return p;
    }

    private PdfPCell celdaHead(String txt, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(txt, f));
        c.setBackgroundColor(new Color(230, 230, 230));
        c.setPadding(5);
        return c;
    }

    private PdfPCell celda(String txt, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(txt != null ? txt : "-", f));
        c.setPadding(5);
        return c;
    }
}
