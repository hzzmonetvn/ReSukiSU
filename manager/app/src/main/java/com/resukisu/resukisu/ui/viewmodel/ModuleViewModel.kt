package com.resukisu.resukisu.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resukisu.resukisu.ksuApp
import com.resukisu.resukisu.ui.util.HanziToPinyin
import com.resukisu.resukisu.ui.util.getRootShell
import com.resukisu.resukisu.ui.util.listModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

/**
 * @author ShirkNeko
 * @date 2025/5/31.
 */
class ModuleViewModel : ViewModel() {

    companion object {
        private const val TAG = "ModuleViewModel"
        private var modules by mutableStateOf<List<ModuleInfo>>(emptyList())
    }

    fun getModuleSize(dirId: String): String {
        return formatFileSize(try {
            val shell = getRootShell()
            val command = "/data/adb/ksu/bin/busybox du -sb /data/adb/modules/$dirId"
            val result = shell.newJob().add(command).to(ArrayList(), null).exec()

            if (result.isSuccess && result.out.isNotEmpty()) {
                val sizeStr = result.out.firstOrNull()?.split("\t")?.firstOrNull()
                sizeStr?.toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(com.resukisu.resukisu.ui.viewmodel.TAG, "计算模块大小失败 $dirId: ${e.message}")
            0L
        })
    }

    class ModuleInfo(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val versionCode: Int,
        val description: String,
        val enabled: Boolean,
        val update: Boolean,
        val remove: Boolean,
        val updateJson: String,
        val hasWebUi: Boolean,
        val hasActionScript: Boolean,
        val metamodule: Boolean,
        val actionIconPath: String?,
        val webUiIconPath: String?,
        val dirId: String, // real module id (dir name)
    ) {
        var moduleUpdate by mutableStateOf<Triple<String, String, String>?>(null)
    }

    var isRefreshing by mutableStateOf(false)
        private set
    var search by mutableStateOf("")

    var sortEnabledFirst by mutableStateOf(false)
    var sortActionFirst by mutableStateOf(false)
    val moduleList by derivedStateOf {
        val comparator =
            compareBy<ModuleInfo>(
                {
                    val executable = it.hasWebUi || it.hasActionScript
                    when {
                        it.metamodule && it.enabled -> 0
                        sortEnabledFirst && sortActionFirst -> when {
                            it.enabled && executable -> 1
                            it.enabled -> 2
                            executable -> 3
                            else -> 4
                        }
                        sortEnabledFirst && !sortActionFirst -> if (it.enabled) 1 else 2
                        !sortEnabledFirst && sortActionFirst -> if (executable) 1 else 2
                        else -> 1
                    }
                },
                { if (sortEnabledFirst) !it.enabled else 0 },
                { if (sortActionFirst) !(it.hasWebUi || it.hasActionScript) else 0 },
            ).thenBy(Collator.getInstance(Locale.getDefault()), ModuleInfo::id)
        modules.filter {
            it.id.contains(search, true) || it.name.contains(search, true) || HanziToPinyin.getInstance()
                .toPinyinString(it.name)?.contains(search, true) == true
        }.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    val haveWebuiModuleList by derivedStateOf {
        moduleList.filter { it.hasWebUi }
    }

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun fetchModuleList(
        manualRefresh: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true

            val oldModuleList = modules

            val start = SystemClock.elapsedRealtime()

            kotlin.runCatching {
                val result = listModules()

                Log.i(TAG, "result: $result")

                val array = JSONArray(result)
                modules = (0 until array.length())
                    .asSequence()
                    .map { array.getJSONObject(it) }
                    .map { obj ->
                        ModuleInfo(
                            obj.getString("id"),
                            obj.optString("name"),
                            obj.optString("author", "Unknown"),
                            obj.optString("version", "Unknown"),
                            obj.getIntCompat("versionCode", 0),
                            obj.optString("description"),
                            obj.getBooleanCompat("enabled"),
                            obj.getBooleanCompat("update"),
                            obj.getBooleanCompat("remove"),
                            obj.optString("updateJson"),
                            obj.getBooleanCompat("web"),
                            obj.getBooleanCompat("action"),
                            obj.getBooleanCompat("metamodule"),
                            obj.optString("actionIcon").takeIf { it.isNotBlank() },
                            obj.optString("webuiIcon").takeIf { it.isNotBlank() },
                            obj.optString("dir_id", obj.getString("id")),
                        )
                    }.toList()

                val moduleList = mutableListOf<String>()
                if (!manualRefresh) {
                    oldModuleList.forEach { module ->
                        moduleList.add(module.id + module.versionCode)
                    }
                }

                modules.forEach { module ->
                    if (!moduleList.contains(module.id + module.versionCode))
                        module.moduleUpdate = checkUpdate(module)
                }

                isNeedRefresh = false
            }.onFailure { e ->
                Log.e(TAG, "fetchModuleList: ", e)
                isRefreshing = false
            }

            // when both old and new is kotlin.collections.EmptyList
            // moduleList update will don't trigger
            if (oldModuleList === modules) {
                isRefreshing = false
            }

            Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, modules: $modules")
        }
    }

    private fun sanitizeVersionString(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    fun checkUpdate(m: ModuleInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        val isCheckUpdateEnabled = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("check_update", true)
        if (!isCheckUpdateEnabled || m.updateJson.isEmpty() || m.remove || m.update || !m.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = m.updateJson
            Log.i(TAG, "checkUpdate url: $url")

            val request = okhttp3.Request.Builder()
                .url(url)
                .build()

            val response = ksuApp.okhttpClient.newCall(request).execute()

            Log.d(TAG, "checkUpdate code: ${response.code}")
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                Log.d(TAG, "checkUpdate failed: ${response.message}")
                ""
            }
        }.getOrElse { e ->
            Log.e(TAG, "checkUpdate exception", e)
            ""
        }

        Log.i(TAG, "checkUpdate result: $result")

        if (result.isEmpty()) {
            return empty
        }

        val updateJson = kotlin.runCatching {
            JSONObject(result)
        }.getOrNull() ?: return empty

        var version = updateJson.optString("version", "")
        version = sanitizeVersionString(version)
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")
        if (versionCode <= m.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }
}

fun ModuleViewModel.ModuleInfo.copy(
    id: String = this.id,
    name: String = this.name,
    author: String = this.author,
    version: String = this.version,
    versionCode: Int = this.versionCode,
    description: String = this.description,
    enabled: Boolean = this.enabled,
    update: Boolean = this.update,
    remove: Boolean = this.remove,
    updateJson: String = this.updateJson,
    hasWebUi: Boolean = this.hasWebUi,
    hasActionScript: Boolean = this.hasActionScript,
    metamodule: Boolean = this.metamodule,
    actionIconPath: String?,
    webUiIconPath: String?,
    dirId: String = this.dirId,
): ModuleViewModel.ModuleInfo {
    return ModuleViewModel.ModuleInfo(
        id, name, author, version, versionCode, description,
        enabled, update, remove, updateJson, hasWebUi, hasActionScript, metamodule,
        actionIconPath, webUiIconPath, dirId
    )
}

private fun JSONObject.getBooleanCompat(key: String, default: Boolean = false): Boolean {
    if (!has(key)) return default
    return when (val value = opt(key)) {
        is Boolean -> value
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        is Number -> value.toInt() != 0
        else -> default
    }
}

private fun JSONObject.getIntCompat(key: String, default: Int = 0): Int {
    if (!has(key)) return default
    return when (val value = opt(key)) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> default
    }
}

/**
 * 格式化文件大小
 */
fun formatFileSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024

    return when {
        bytes >= tb -> "%.2f TB".format(bytes / tb)
        bytes >= gb -> "%.2f GB".format(bytes / gb)
        bytes >= mb -> "%.2f MB".format(bytes / mb)
        bytes >= kb -> "%.2f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}
