package com.mediatoolkit.pareco.transfer;

import com.mediatoolkit.pareco.restclient.DownloadClient.DownloadSessionClient;
import com.mediatoolkit.pareco.restclient.UploadClient.UploadSessionClient;
import com.mediatoolkit.pareco.util.Util;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 08/11/2018
 */
@Component
public class ExitTransferAborter {

	public void registerAbort(UploadSessionClient sessionClient) {
		registerAbort(sessionClient::abortUpload);
	}

	public void registerAbort(DownloadSessionClient sessionClient) {
		registerAbort(sessionClient::abortDownload);
	}

	private void registerAbort(Runnable abortAction) {
		Runtime.getRuntime().addShutdownHook(new Thread(
			() -> Util.runIgnoreException(abortAction::run)
		));
	}
}
