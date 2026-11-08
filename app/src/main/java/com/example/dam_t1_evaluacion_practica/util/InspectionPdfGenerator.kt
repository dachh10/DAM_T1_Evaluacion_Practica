package com.example.dam_t1_evaluacion_practica.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InspectionPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    private val COL_NAVY        = Color.rgb(11, 25, 44)
    private val COL_AMBER       = Color.rgb(245, 158, 11)
    private val COL_GREEN       = Color.rgb(22, 163, 74)
    private val COL_RED         = Color.rgb(220, 38, 38)
    private val COL_GRAY_DARK   = Color.rgb(30, 41, 59)
    private val COL_GRAY_MID    = Color.rgb(100, 116, 139)
    private val COL_GRAY_LIGHT  = Color.rgb(241, 245, 249)

    private fun paint(color: Int, size: Float, bold: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun rectPaint(color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun generate(context: Context, record: InspectionSessionRecord): Uri? {
        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var canvas = page.canvas
        var y = MARGIN

        y = drawHeader(canvas, record, y)
        y = drawSummary(canvas, record, y)
        y = drawSectionTitle(canvas, "Modulos Inspeccionados", y)

        for (module in record.modules) {
            if (y > PAGE_HEIGHT - 80) {
                doc.finishPage(page); pageNum++
                page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
            }
            y = drawModuleRow(canvas, module.moduleTitle, module.score, module.criticalStop, y)
        }

        if (record.findings.isNotEmpty()) {
            y += 12f
            if (y > PAGE_HEIGHT - 80) {
                doc.finishPage(page); pageNum++
                page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
            }
            y = drawSectionTitle(canvas, "Hallazgos No Conformes (${record.findings.size})", y)
            for (finding in record.findings) {
                val hasPhoto = !finding.photoUri.isNullOrBlank()
                val estimatedH = if (hasPhoto) 190f else 75f
                if (y + estimatedH > PAGE_HEIGHT - MARGIN - 40f) {
                    doc.finishPage(page); pageNum++
                    page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
                }
                y = drawFinding(context, canvas, finding, y)
            }
        }

        if (record.normCodes.isNotEmpty()) {
            y += 12f
            if (y > PAGE_HEIGHT - 80) {
                doc.finishPage(page); pageNum++
                page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
            }
            y = drawSectionTitle(canvas, "Normas Oficiales Aplicadas", y)
            for (code in record.normCodes) {
                if (y > PAGE_HEIGHT - 40) {
                    doc.finishPage(page); pageNum++
                    page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
                }
                y = drawNormRow(canvas, code, y)
            }
        }

        drawFooter(canvas, record)
        doc.finishPage(page)
        return saveDocument(context, doc, record.folio)
    }

    private fun newPage(doc: PdfDocument, num: Int): PdfDocument.Page {
        val pi = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, num).create()
        return doc.startPage(pi).also { it.canvas.drawColor(Color.WHITE) }
    }

    private fun drawHeader(canvas: Canvas, record: InspectionSessionRecord, startY: Float): Float {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 72f, rectPaint(COL_NAVY))
        canvas.drawText("DICTAMEN DE AUDITORIA DE SEGURIDAD", MARGIN, 30f, paint(Color.WHITE, 13f, true))
        canvas.drawText(record.folio, MARGIN, 52f, paint(COL_AMBER, 11f, true))
        val dateStr = formatDate(record.timestamp)
        val dp = paint(Color.WHITE, 9f)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dp.measureText(dateStr), 44f, dp)
        var y = 88f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28f, rectPaint(COL_GRAY_LIGHT))
        canvas.drawText("Empresa: ${record.companyName}", MARGIN + 8f, y + 18f, paint(COL_GRAY_DARK, 9f, true))
        val zoneStr = "Zona: ${record.zoneName}  |  Tipo: ${record.auditType}"
        val zp = paint(COL_GRAY_MID, 9f)
        canvas.drawText(zoneStr, PAGE_WIDTH - MARGIN - 8f - zp.measureText(zoneStr), y + 18f, zp)
        return y + 36f
    }

    private fun drawSummary(canvas: Canvas, record: InspectionSessionRecord, startY: Float): Float {
        var y = startY + 8f
        val accent = when {
            record.criticalStop    -> COL_RED
            record.reinspectionDue -> COL_AMBER
            else                   -> COL_GREEN
        }
        val statusLabel = when {
            record.criticalStop    -> "PARO CRITICO"
            record.reinspectionDue -> "REINSPECCION REQUERIDA"
            else                   -> "CONFORME"
        }
        val boxSize = 70f
        val scoreRect = RectF(MARGIN, y, MARGIN + boxSize, y + boxSize)
        canvas.drawRoundRect(scoreRect, 12f, 12f, rectPaint(accent))
        val scoreText = "${record.overallScore}%"
        val sp = paint(Color.WHITE, 18f, true)
        canvas.drawText(scoreText, MARGIN + (boxSize - sp.measureText(scoreText)) / 2f, y + 44f, sp)
        val glp = paint(Color.WHITE, 8f)
        canvas.drawText("Global", MARGIN + (boxSize - glp.measureText("Global")) / 2f, y + 56f, glp)

        val infoX = MARGIN + boxSize + 12f
        canvas.drawText(statusLabel, infoX, y + 18f, paint(accent, 11f, true))
        canvas.drawText("Inspector: ${record.inspectorName}", infoX, y + 34f, paint(COL_GRAY_DARK, 9f))
        canvas.drawText("Cedula: ${record.inspectorCedula}", infoX, y + 48f, paint(COL_GRAY_MID, 9f))
        record.latitude?.let { lat ->
            record.longitude?.let { lng ->
                canvas.drawText("GPS: %.4f N, %.4f W".format(lat, lng), infoX, y + 62f, paint(COL_GRAY_MID, 8f))
            }
        }

        y += boxSize + 16f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = COL_GRAY_LIGHT; strokeWidth = 1f })
        return y + 10f
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, startY: Float): Float {
        val y = startY + 6f
        canvas.drawRect(MARGIN, y, MARGIN + 4f, y + 16f, rectPaint(COL_AMBER))
        canvas.drawText(title, MARGIN + 10f, y + 13f, paint(COL_GRAY_DARK, 11f, true))
        return y + 26f
    }

    private fun drawModuleRow(canvas: Canvas, title: String, score: Int, critical: Boolean, startY: Float): Float {
        val rowH = 28f
        val accent = if (critical) COL_RED else if (score >= 80) COL_GREEN else COL_AMBER
        canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + rowH, rectPaint(COL_GRAY_LIGHT))
        canvas.drawText(title, MARGIN + 8f, startY + 18f, paint(COL_GRAY_DARK, 9f))
        val sp = paint(accent, 10f, true)
        val scoreStr = "$score%"
        canvas.drawText(scoreStr, PAGE_WIDTH - MARGIN - sp.measureText(scoreStr) - 8f, startY + 18f, sp)
        return startY + rowH + 4f
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun drawFinding(context: Context, canvas: Canvas, finding: SessionFinding, startY: Float): Float {
        var y = startY
        val badgeColor = when (finding.criticalityLevel.name) {
            "HIGH"   -> COL_RED
            "MEDIUM" -> COL_AMBER
            else     -> COL_GREEN
        }
        canvas.drawRect(MARGIN, y + 2f, MARGIN + 4f, y + 54f, rectPaint(badgeColor))
        canvas.drawText(finding.itemDescription.take(80), MARGIN + 10f, y + 14f, paint(COL_GRAY_DARK, 9f, true))
        val gpsInfo = if (finding.latitude != null && finding.longitude != null) {
            "  *  GPS: %.4f N, %.4f W".format(finding.latitude, finding.longitude)
        } else ""
        canvas.drawText("${finding.normCode}   ${formatDate(finding.timestamp)}$gpsInfo", MARGIN + 10f, y + 28f, paint(COL_GRAY_MID, 8f))
        finding.notes?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("Nota: ${it.take(90)}", MARGIN + 10f, y + 42f, paint(COL_GRAY_MID, 8f))
        }
        y += 56f
        finding.photoUri?.takeIf { it.isNotBlank() }?.let { uriStr ->
            val bitmap = withContext(Dispatchers.IO) {
                when {
                    uriStr.startsWith("https://") -> {
                        RemoteImageLoader.load(uriStr, context)
                            ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    }
                    uriStr.startsWith("content://") -> {
                        val uri = Uri.parse(uriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    else -> {
                        val uri = Uri.parse(uriStr)
                        BitmapFactory.decodeFile(uri.path ?: uriStr)
                            ?: context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                    }
                }
            }
            val bmp = bitmap
            if (bmp != null) {
                val maxW = 200f
                val maxH = 110f
                val scaleW = maxW / bmp.width.toFloat()
                val scaleH = maxH / bmp.height.toFloat()
                val scale = minOf(scaleW, scaleH)
                val imgW = bmp.width * scale
                val imgH = bmp.height * scale
                val scaled = Bitmap.createScaledBitmap(bmp, imgW.toInt().coerceAtLeast(1), imgH.toInt().coerceAtLeast(1), true)
                canvas.drawBitmap(scaled, MARGIN + 10f, y + 2f, null)
                y += imgH + 8f
            }
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = COL_GRAY_LIGHT; strokeWidth = 0.5f })
        return y + 8f
    }

    private fun drawNormRow(canvas: Canvas, code: String, startY: Float): Float {
        canvas.drawText("* $code", MARGIN + 8f, startY + 12f, paint(COL_GRAY_DARK, 9f))
        return startY + 18f
    }

    private fun drawFooter(canvas: Canvas, record: InspectionSessionRecord) {
        val y = PAGE_HEIGHT - 36f
        canvas.drawLine(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y - 10f, Paint().apply { color = COL_NAVY; strokeWidth = 1f })
        canvas.drawText(
            "Inspector: ${record.inspectorName}  *  Cedula: ${record.inspectorCedula}  *  ${formatDate(record.timestamp)}",
            MARGIN, y + 4f, paint(COL_GRAY_MID, 8f)
        )
        val gen = "Generado por InspeccionesMX"
        val gp = paint(COL_AMBER, 8f, true)
        canvas.drawText(gen, PAGE_WIDTH - MARGIN - gp.measureText(gen), y + 4f, gp)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun saveDocument(context: Context, doc: PdfDocument, folio: String): Uri? {
        val fileName = "Dictamen_${folio.replace("/", "-")}_${System.currentTimeMillis()}.pdf"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, cv, null, null)
                doc.close(); uri
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = java.io.File(dir, fileName)
                file.outputStream().use { doc.writeTo(it) }
                doc.close(); Uri.fromFile(file)
            }
        } catch (e: Exception) {
            doc.close(); null
        }
    }

    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX")).format(Date(ts))
}