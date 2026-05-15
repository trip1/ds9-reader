package com.example.ds9reader.platform

import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSFileProviderDomain
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsDirectoryKey
import platform.Foundation.NSURLIsRegularFileKey
import platform.Foundation.pathExtension
import platform.Foundation.path
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeContent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFilePicker : FilePicker {

    private var presentingController: UIViewController? = null

    override suspend fun pickEpubFile(): PickerResult {
        return suspendCoroutine { continuation ->
            val epubType = UTType("org.idpf.epub-container")
                ?: UTType("org.idpf.epub")
                ?: UTTypeContent

            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(epubType)
            ).apply {
                allowsMultipleSelection = false
                delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAtURLs: List<*>
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val urls = didPickDocumentsAtURLs as List<NSURL>
                        if (urls.isEmpty()) {
                            continuation.resume(PickerResult.Cancelled)
                        } else {
                            val paths = urls.mapNotNull { it.path }
                            if (paths.isEmpty()) {
                                continuation.resume(PickerResult.Error("Could not resolve file path"))
                            } else {
                                continuation.resume(PickerResult.Success(paths))
                            }
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        continuation.resume(PickerResult.Cancelled)
                    }
                }
            }

            // Get top view controller to present
            val rootVC = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(picker, true, null)
        }
    }

    override suspend fun pickEpubDirectory(): PickerResult {
        return suspendCoroutine { continuation ->
            val folderType = UTTypeFolder

            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(folderType)
            ).apply {
                allowsMultipleSelection = false
                delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAtURLs: List<*>
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val urls = didPickDocumentsAtURLs as List<NSURL>
                        if (urls.isEmpty()) {
                            continuation.resume(PickerResult.Cancelled)
                        } else {
                            val paths = urls.mapNotNull { it.path }
                            continuation.resume(PickerResult.Success(paths))
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        continuation.resume(PickerResult.Cancelled)
                    }
                }
            }

            val rootVC = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(picker, true, null)
        }
    }
}
