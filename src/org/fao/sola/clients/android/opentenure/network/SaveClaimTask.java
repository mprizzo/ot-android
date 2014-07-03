/**
 * ******************************************************************************************
 * Copyright (C) 2014 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.fao.sola.clients.android.opentenure.network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.fao.sola.clients.android.opentenure.OpenTenureApplication;
import org.fao.sola.clients.android.opentenure.R;
import org.fao.sola.clients.android.opentenure.ViewHolder;
import org.fao.sola.clients.android.opentenure.filesystem.FileSystemUtilities;
import org.fao.sola.clients.android.opentenure.filesystem.json.JsonUtilities;
import org.fao.sola.clients.android.opentenure.model.Claim;
import org.fao.sola.clients.android.opentenure.model.ClaimStatus;
import org.fao.sola.clients.android.opentenure.network.API.CommunityServerAPI;
import org.fao.sola.clients.android.opentenure.network.response.Attachment;
import org.fao.sola.clients.android.opentenure.network.response.SaveClaimResponse;
import org.fao.sola.clients.android.opentenure.network.response.ViewHolderResponse;

import android.view.View;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * The save Claim upload all the meta data about the Claim to the community
 * server, sending the claim.json . If the server is missing some claims for
 * that claim ,
 * 
 * **/
public class SaveClaimTask extends AsyncTask<Object, Void, ViewHolderResponse> {

	@Override
	protected ViewHolderResponse doInBackground(Object... params) {
		// TODO Auto-generated method stub
		String claimId = (String) params[0];
		ViewHolder vh = (ViewHolder) params[1];
		String json = FileSystemUtilities.getJsonClaim(claimId);
		
		SaveClaimResponse res = CommunityServerAPI.saveClaim(json);
		res.setClaimId(claimId);

		ViewHolderResponse vhr = new ViewHolderResponse();
		vhr.setRes(res);
		vhr.setVh(vh);

		return vhr;

	}

	// @Override
	// protected SaveClaimResponse doInBackground(ViewHolder... params) {
	//

	// }

	protected void onPostExecute(final ViewHolderResponse vhr) {

		Toast toast;

		SaveClaimResponse res = (SaveClaimResponse) vhr.getRes();

		Claim claim = Claim.getClaim(res.getClaimId());

		switch (res.getHttpStatusCode()) {

		case 100: {
			/* UnknownHostException: */

			claim.setStatus(ClaimStatus._UPLOAD_INCOMPLETE);
			claim.update();

			toast = Toast.makeText(
					OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submission_error)
							+ "  "
							+ OpenTenureApplication
									.getContext()
									.getResources()
									.getString(
											R.string.message_connection_error),
					Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			int progress = FileSystemUtilities.getUploadProgress(claim);

			vh.getStatus().setText(ClaimStatus._UPLOAD_INCOMPLETE);
			vh.getStatus().setVisibility(View.VISIBLE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;

		}

		case 105: {
			/* IOException: */

			if (claim.getStatus().equals(ClaimStatus._UPLOADING)) {
				claim.setStatus(ClaimStatus._UPLOAD_ERROR);
				claim.update();
			}

			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submission_error)
							+ " " + res.getMessage(), Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			int progress = FileSystemUtilities.getUploadProgress(claim);

			vh.getStatus().setText(ClaimStatus._UPLOAD_ERROR);
			vh.getStatus().setVisibility(View.VISIBLE);
			vh.getBar().setVisibility(View.GONE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;

		}
		case 110: {
			/* IOException: */

			if (claim.getStatus().equals(ClaimStatus._UPLOADING)) {
				claim.setStatus(ClaimStatus._UPLOAD_ERROR);
				claim.update();
			}

			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submission_error)
							+ " " + res.getMessage(), Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			int progress = FileSystemUtilities.getUploadProgress(claim);

			vh.getStatus().setText(ClaimStatus._UPLOAD_ERROR);
			vh.getStatus().setVisibility(View.VISIBLE);
			vh.getBar().setVisibility(View.GONE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;

		}

		case 200: {

			/* OK */

			try {

				TimeZone tz = TimeZone.getTimeZone("UTC");
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss");
				sdf.setTimeZone(tz);
				Date date = sdf.parse(res.getChallengeExpiryDate());

				claim.setChallengeExpiryDate(new java.sql.Date(date.getTime()));

				claim.setStatus(ClaimStatus._UNMODERATED);
				claim.update();

			} catch (Exception e) {
				Log.d("CommunityServerAPI",
						"SAVE CLAIM JSON RESPONSE " + res.getMessage());
				e.printStackTrace();
			}

			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submitted),
					Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();
			vh.getBar().setVisibility(View.GONE);

			int days = JsonUtilities.remainingDays(claim
					.getChallengeExpiryDate());

			vh.getChallengeExpiryDate().setText(
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_remaining_days)
							+ days);

			vh.getChallengeExpiryDate().setVisibility(View.VISIBLE);
			vh.getStatus().setVisibility(View.GONE);

			vh.getIconLocal().setVisibility(View.GONE);
			vh.getIconUnmoderated().setVisibility(View.VISIBLE);
			break;
		}

		case 403:

			/* Error Login */

			Log.d("CommunityServerAPI",
					"SAVE CLAIM JSON RESPONSE " + res.getMessage());

			toast = Toast
					.makeText(
							OpenTenureApplication.getContext(),
							R.string.message_login_no_more_valid + "  "
									+ res.getHttpStatusCode() + "  "
									+ res.getMessage(), Toast.LENGTH_LONG);
			toast.show();

			OpenTenureApplication.setLoggedin(false);

			break;

		case 404: {

			/* Error Login */

			Log.d("CommunityServerAPI",
					"SAVE CLAIM JSON RESPONSE " + res.getMessage());

			toast = Toast
					.makeText(
							OpenTenureApplication.getContext(),
							OpenTenureApplication
									.getContext()
									.getResources()
									.getString(
											R.string.message_submission_error)
									+ " "
									+ OpenTenureApplication
											.getContext()
											.getResources()
											.getString(
													R.string.message_service_not_available),
							Toast.LENGTH_SHORT);
			toast.show();

			claim.setStatus(ClaimStatus._UPLOAD_ERROR);
			claim.update();

			ViewHolder vh = vhr.getVh();

			vh.getStatus().setText(ClaimStatus._UPLOAD_ERROR);
			vh.getStatus().setVisibility(View.VISIBLE);
			vh.getBar().setVisibility(View.GONE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;
		}

		case 452: {

			/* Missing Attachments */

			if (claim.getStatus().equals(ClaimStatus._CREATED)) {
				claim.setStatus(ClaimStatus._UPLOADING);
				claim.update();
			}
			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_uploading),
					Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			int progress = FileSystemUtilities.getUploadProgress(claim);

			System.out
					.println("SaveClaimTask Qui il progress e' : " + progress);

			vh.getStatus().setText(
					ClaimStatus._UPLOADING + ": " + progress + " %");
			vh.getStatus().setVisibility(View.VISIBLE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			List<Attachment> list = res.getAttachments();

			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Attachment attachment = (Attachment) iterator.next();

				SaveAttachmentTask saveAttachmentTask = new SaveAttachmentTask();
				saveAttachmentTask.executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR, attachment.getId(),
						vhr.getVh());

			}

			break;
		}
		case 450: {

			Log.d("CommunityServerAPI",
					"SAVE CLAIM JSON RESPONSE " + res.getMessage());

			claim.setStatus(ClaimStatus._UPLOAD_ERROR);
			claim.update();

			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submission_error)
							+ " ," + res.getMessage(), Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			vh.getStatus().setText(ClaimStatus._UPLOAD_ERROR);
			vh.getStatus().setVisibility(View.VISIBLE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;
		}
		case 400:

			Log.d("CommunityServerAPI",
					"SAVE CLAIM JSON RESPONSE " + res.getMessage());

			claim.setStatus(ClaimStatus._UPLOAD_ERROR);
			claim.update();

			toast = Toast.makeText(OpenTenureApplication.getContext(),
					OpenTenureApplication.getContext().getResources()
							.getString(R.string.message_submission_error)
							+ " ," + res.getMessage(), Toast.LENGTH_SHORT);
			toast.show();

			ViewHolder vh = vhr.getVh();

			vh.getStatus().setText(ClaimStatus._UPLOAD_ERROR);
			vh.getStatus().setVisibility(View.VISIBLE);
			vh.getBar().setVisibility(View.GONE);

			vh.getIconLocal().setVisibility(View.VISIBLE);
			vh.getIconUnmoderated().setVisibility(View.GONE);

			break;

		default:
			break;
		}

		return;

	}

}
