/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.sync

import android.content.Context
import android.net.Uri
import com.example.focuspause.data.FocusRepository
import com.example.focuspause.data.SyncMerger
import com.example.focuspause.data.SyncPayload
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Sincroniza datos entre el celular y el reloj con la Data Layer de Wear OS.
 * Ambas apps deben tener el mismo applicationId y la misma firma, y los dispositivos deben estar emparejados.
 * Si Google Play Services no está disponible, simplemente no sincroniza (la app sigue funcionando).
 */
class WearSyncManager(
    context: Context,
    private val repository: FocusRepository,
    private val scope: CoroutineScope,
    /** Se llama (en el hilo principal) cuando llegaron datos nuevos del otro dispositivo. */
    private val onMerged: () -> Unit
) : DataClient.OnDataChangedListener {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private var dataClient: DataClient? = null

    fun start() {
        val client = runCatching { Wearable.getDataClient(appContext) }.getOrNull() ?: return
        dataClient = client
        runCatching { client.addListener(this) }
        scope.launch {
            // Al abrir la app: recoger lo que el otro dispositivo ya publicó y publicar lo nuestro.
            runCatching { pullAndMerge() }
            push()
        }
    }

    fun stop() {
        runCatching { dataClient?.removeListener(this) }
    }

    /** Publica el estado local para que el otro dispositivo lo reciba. */
    fun push() {
        val client = dataClient ?: return
        scope.launch {
            runCatching {
                val snapshot = repository.snapshot()
                val request = PutDataMapRequest.create(PATH).apply {
                    dataMap.putString(KEY_PAYLOAD, json.encodeToString(snapshot))
                }.asPutDataRequest().setUrgent()
                withContext(Dispatchers.IO) { Tasks.await(client.putDataItem(request)) }
            }
        }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        // El buffer solo es válido dentro del callback: extraemos los textos ahora.
        val payloads = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == PATH }
            .mapNotNull { DataMapItem.fromDataItem(it.dataItem).dataMap.getString(KEY_PAYLOAD) }
        if (payloads.isEmpty()) return
        scope.launch { runCatching { mergeRemote(payloads) } }
    }

    private suspend fun pullAndMerge() {
        val client = dataClient ?: return
        val payloads = withContext(Dispatchers.IO) {
            // Sin host en la URI = datos de todos los dispositivos.
            val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH).build()
            val buffer = Tasks.await(client.getDataItems(uri))
            try {
                buffer.mapNotNull { DataMapItem.fromDataItem(it).dataMap.getString(KEY_PAYLOAD) }
            } finally {
                buffer.release()
            }
        }
        mergeRemote(payloads)
    }

    private suspend fun mergeRemote(rawPayloads: List<String>) {
        val remotes = rawPayloads.mapNotNull {
            runCatching { json.decodeFromString<SyncPayload>(it) }.getOrNull()
        }
        if (remotes.isEmpty()) return

        val local = repository.snapshot()
        val base = SyncMerger.merge(local, local)               // normaliza el orden local
        val merged = remotes.fold(base) { acc, remote -> SyncMerger.merge(acc, remote) }
        if (merged != base) {
            repository.applySnapshot(merged)
            onMerged()
            push()                                              // para que el otro también reciba lo nuestro
        }
    }

    private companion object {
        const val PATH = "/focuspause/state"
        const val KEY_PAYLOAD = "payload"
    }
}
