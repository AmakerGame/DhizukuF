package com.EdS.DhizukuF.dish

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.io.OutputStream

/** Writes the `dish` launcher and `dish_dhizukuf.dex` (the terminal client) into a folder picked by the user. */
object DishExporter {
    const val SCRIPT_NAME = "dish"
    const val DEX_NAME = "dish_dhizukuf.dex"
    private const val MIME = "application/octet-stream"

    @Throws(IOException::class)
    fun export(context: Context, tree: Uri) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)

        val script = context.assets.open(SCRIPT_NAME).bufferedReader().use { it.readText() }
            .replace("__PACKAGE__", context.packageName)
            .replace("__SERVER_UID__", context.applicationInfo.uid.toString())

        write(context, tree, treeId, parent, SCRIPT_NAME) { it.write(script.toByteArray()) }
        write(context, tree, treeId, parent, DEX_NAME) { out ->
            context.assets.open(DEX_NAME).use { it.copyTo(out) }
        }
    }

    private fun write(
        context: Context,
        tree: Uri,
        treeId: String,
        parent: Uri,
        name: String,
        block: (OutputStream) -> Unit
    ) {
        val resolver = context.contentResolver
        findChild(context, tree, treeId, name)?.let {
            DocumentsContract.deleteDocument(resolver, it)
        }
        val doc = DocumentsContract.createDocument(resolver, parent, MIME, name)
            ?: throw IOException("cannot create $name")
        val stream = resolver.openOutputStream(doc, "wt") ?: throw IOException("cannot open $name")
        stream.use(block)
    }

    private fun findChild(context: Context, tree: Uri, treeId: String, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                }
            }
        }
        return null
    }
}
