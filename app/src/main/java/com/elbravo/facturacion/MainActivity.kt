package com.elbravo.facturacion

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { InvoiceScreen(this) } }
    }
}
data class Item(val name: String, val price: Double, val qty: Int)

@Composable
fun InvoiceScreen(ctx: Context) {
    var customer by remember { mutableStateOf("Consumidor final") }
    var product by remember { mutableStateOf("POLLO COMPLETO") }
    var price by remember { mutableStateOf("18.00") }
    var qty by remember { mutableStateOf("1") }
    var items by remember { mutableStateOf(listOf<Item>()) }
    var next by remember { mutableIntStateOf(ctx.getSharedPreferences("bravo", 0).getInt("next", 1)) }
    var note by remember { mutableStateOf("") }
    val total = items.sumOf { it.price * it.qty }
    val number = "FAC-${next.toString().padStart(5, '0')}"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("EL BRAVO ROSTIZADO", style = MaterialTheme.typography.headlineSmall)
        Text("Col. Gloria, calle a Mariona #8, Mejicanos, San Salvador\nWhatsApp: 7627-0874\nelbravorostizado@gmail.com")
        Text("Nueva factura $number", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(customer, { customer = it }, label = { Text("Cliente") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(product, { product = it }, label = { Text("Producto / servicio") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(price, { price = it }, label = { Text("Precio $") }, modifier = Modifier.weight(1f))
            OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("Cantidad") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            val p = price.replace(',', '.').toDoubleOrNull()
            val q = qty.toIntOrNull()
            if (product.isNotBlank() && p != null && q != null && p >= 0 && q > 0) {
                items = items + Item(product.trim(), p, q)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Agregar producto") }

        items.forEachIndexed { i, item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.qty} x ${item.name}")
                Text("$%.2f".format(Locale.US, item.qty * item.price))
                TextButton(onClick = { items = items.filterIndexed { n, _ -> n != i } }) { Text("Quitar") }
            }
        }
        OutlinedTextField(note, { note = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
        Text("TOTAL: $%.2f".format(Locale.US, total), style = MaterialTheme.typography.headlineMedium)
        Text("Sin cálculo de IVA. Datos guardados localmente en este teléfono.")
        Button(enabled = items.isNotEmpty(), onClick = {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "SV")).format(Date())
            val file = File(ctx.cacheDir, "$number.pdf")
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val paint = Paint()
            var y = 50f
            fun line(value: String, size: Float = 12f) {
                paint.textSize = size
                canvas.drawText(value.take(90), 40f, y, paint)
                y += 22f
            }
            line("EL BRAVO ROSTIZADO", 20f)
            line("Col. Gloria, calle a Mariona #8, Mejicanos, San Salvador")
            line("WhatsApp: 7627-0874 | elbravorostizado@gmail.com")
            line("FACTURA $number", 18f)
            line("Fecha: $date")
            line("Cliente: $customer")
            line("----------------------------------------------")
            items.forEach { line("${it.qty} x ${it.name}   $%.2f".format(Locale.US, it.qty * it.price)) }
            line("TOTAL: $%.2f".format(Locale.US, total), 18f)
            if (note.isNotBlank()) line("Notas: $note")
            line("Gracias por su compra.")
            doc.finishPage(page)
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Factura $number")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "Compartir factura PDF"))
            ctx.getSharedPreferences("bravo", 0).edit().putInt("next", next + 1).apply()
            next++
            items = emptyList()
            customer = "Consumidor final"
            note = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Guardar y compartir PDF") }
    }
}
