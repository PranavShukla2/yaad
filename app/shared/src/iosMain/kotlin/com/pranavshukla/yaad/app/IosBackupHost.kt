package com.pranavshukla.yaad.app

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

/** Saves backups through the share sheet ("Save to Files") and opens them with the document picker. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal object IosBackupHost : BackupHost {
    private var picker: Picker? = null // the picker holds its delegate weakly

    @Suppress("DEPRECATION")
    private fun top(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }

    override fun save(fileName: String, content: String) {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        (NSString.create(string = content)).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        val share = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        val host = top() ?: return
        // On iPad the sheet is a popover and needs an anchor.
        share.popoverPresentationController?.sourceView = host.view
        host.presentViewController(share, animated = true, completion = null)
    }

    override fun open(onOpened: (String) -> Unit) {
        val controller = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = true)
        val delegate = Picker(onOpened)
        picker = delegate
        controller.delegate = delegate
        top()?.presentViewController(controller, animated = true, completion = null)
    }

    private class Picker(val onOpened: (String) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
            NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null)?.let { onOpened(it) }
        }
    }
}
