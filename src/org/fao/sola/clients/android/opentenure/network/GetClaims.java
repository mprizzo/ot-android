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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.fao.sola.clients.android.opentenure.filesystem.FileSystemUtilities;
import org.fao.sola.clients.android.opentenure.filesystem.json.JsonUtilities;
import org.fao.sola.clients.android.opentenure.filesystem.json.model.Attachment;
import org.fao.sola.clients.android.opentenure.filesystem.json.model.Claim;
import org.fao.sola.clients.android.opentenure.filesystem.json.model.Claimant;
import org.fao.sola.clients.android.opentenure.filesystem.json.model.Share;
import org.fao.sola.clients.android.opentenure.model.AttachmentStatus;
import org.fao.sola.clients.android.opentenure.model.Owner;
import org.fao.sola.clients.android.opentenure.model.Person;
import org.fao.sola.clients.android.opentenure.model.Vertex;
import org.fao.sola.clients.android.opentenure.network.API.CommunityServerAPI;
import android.util.Log;

/**
 * Loop on the list of Claims to download, retrieving them once for time and
 * adding them on the local DB. The necessary file system is created indeed
 * */
public class GetClaims {

	public static boolean execute(
			org.fao.sola.clients.android.opentenure.network.response.Claim[] params) {

		boolean success = true;

		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf.setTimeZone(tz);

		for (int i = 0; i < params.length; i++) {
			org.fao.sola.clients.android.opentenure.network.response.Claim claim = (org.fao.sola.clients.android.opentenure.network.response.Claim) params[i];

			Claim downloadedClaim = CommunityServerAPI.getClaim(claim.getId());

			if (downloadedClaim == null)
				success = false;

			/**
			 * 
			 * Parsing the downloaded Claim and saving it to DB
			 * 
			 **/

			List<org.fao.sola.clients.android.opentenure.model.Attachment> attachmentsDB = new ArrayList<org.fao.sola.clients.android.opentenure.model.Attachment>();
			List<org.fao.sola.clients.android.opentenure.model.AdditionalInfo> additionalInfoDBList = new ArrayList<org.fao.sola.clients.android.opentenure.model.AdditionalInfo>();

			org.fao.sola.clients.android.opentenure.model.Claim claimDB = new org.fao.sola.clients.android.opentenure.model.Claim();

			/*
			 * Temporary disable
			 */
			// List<AdditionalInfo> metadataList;
			// if ((metadataList = claim.getAdditionaInfo()) != null) {
			//
			// for (Iterator iterator = metadataList.iterator(); iterator
			// .hasNext();) {
			// AdditionalInfo additionalInfo = (AdditionalInfo) iterator
			// .next();
			//
			// org.fao.sola.clients.android.opentenure.model.Metadata metadataDB
			// =
			// new Metadata();
			//
			// metadataDB.setClaimId(claim.getId());
			// metadataDB.setMetadataId(additionalInfo.getMetadataId());
			// metadataDB.setName(additionalInfo.getName());
			// metadataDB.setValue(additionalInfo.getValue());
			//
			// metadataDBList.add(metadataDB);
			//
			// }
			// }

			/*
			 * First of all che if claim is challenged In case of challenge,
			 * need to download ad save the claim challenging
			 * 
			 * 
			 * 
			 * 
			 * We should set the challenged claim but if is not in the right
			 * order it will be there a problem
			 */

			if (downloadedClaim.getChallengedClaimId() != null
					&& !downloadedClaim.getChallengedClaimId().equals("")) {

				/*
				 * Check if the claim is already present locally
				 */
				org.fao.sola.clients.android.opentenure.model.Claim challenged = org.fao.sola.clients.android.opentenure.model.Claim
						.getClaim(downloadedClaim.getChallengedClaimId());
				if (challenged == null) {

					/*
					 * here the case in which the claim challenged is not
					 * already present locally
					 */

					org.fao.sola.clients.android.opentenure.network.response.Claim[] claimCarray = new org.fao.sola.clients.android.opentenure.network.response.Claim[1];

					org.fao.sola.clients.android.opentenure.network.response.Claim toRetrieve = new org.fao.sola.clients.android.opentenure.network.response.Claim();

					toRetrieve.setId(downloadedClaim.getChallengedClaimId());

					claimCarray[0] = toRetrieve;

					/*
					 * Making a recoursive call
					 */
					GetClaims.execute(claimCarray);

					claimDB.setChallengedClaim(org.fao.sola.clients.android.opentenure.model.Claim
							.getClaim(downloadedClaim.getChallengedClaimId()));

				} else {

					claimDB.setChallengedClaim(org.fao.sola.clients.android.opentenure.model.Claim
							.getClaim(downloadedClaim.getChallengedClaimId()));

				}

			}

			Claimant claimant = downloadedClaim.getClaimant();

			Person person = new Person();
			person.setContactPhoneNumber(claimant.getPhone());

			Date birth = null;
			try {
				// birth = df.parse(claimant.getBirthDate());

				Calendar cal = JsonUtilities
						.toCalendar(claimant.getBirthDate());
				birth = cal.getTime();

				if (birth != null)
					person.setDateOfBirth(new java.sql.Date(birth.getTime()));
				else
					person.setDateOfBirth(new java.sql.Date(2000, 2, 3));

				// *****Qui ho un problema perche' la data di nascita puo' nn
				// esserci.
				// Nn ci serebbe motivo contrario*************************////

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				Log.d("CommunityServerAPI",
						"ERROR DOWNLOADING  CLAIM " + claim.getId());

				success = false;
			}

			try {

				person.setEmailAddress(claimant.getEmail());
				person.setFirstName(claimant.getName());
				person.setGender(claimant.getGenderCode());
				person.setLastName(claimant.getLastName());
				person.setMobilePhoneNumber(claimant.getMobilePhone());
				person.setPersonId(claimant.getId());
				// person.setPlaceOfBirth(claimant.getPlaceOfBirth());
				person.setPostalAddress(claimant.getAddress());

				claimDB.setAttachments(attachmentsDB);

				claimDB.setClaimId(downloadedClaim.getId());
				claimDB.setAdditionalInfo(additionalInfoDBList);
				claimDB.setName(downloadedClaim.getDescription());

				Date date = sdf.parse(downloadedClaim.getChallengeExpiryDate());
				claimDB.setChallengeExpiryDate(new java.sql.Date(date.getTime()));

				claimDB.setPerson(person);
				claimDB.setStatus(downloadedClaim.getStatusCode());
				claimDB.setType(downloadedClaim.getTypeCode());

				Person.createPerson(person);

				org.fao.sola.clients.android.opentenure.model.Claim
						.createClaim(claimDB);

				if (downloadedClaim.getGpsGeometry().startsWith("POINT"))
					Vertex.storeWKT(claimDB.getClaimId(),
							downloadedClaim.getMappedGeometry(),
							downloadedClaim.getMappedGeometry());
				else
					Vertex.storeWKT(claimDB.getClaimId(),
							downloadedClaim.getMappedGeometry(),
							downloadedClaim.getGpsGeometry());

				List<Attachment> attachments = downloadedClaim.getAttachments();
				for (Iterator<Attachment> iterator = attachments.iterator(); iterator
						.hasNext();) {

					org.fao.sola.clients.android.opentenure.model.Attachment attachmentDB = new org.fao.sola.clients.android.opentenure.model.Attachment();
					Attachment attachment = (Attachment) iterator.next();

					attachmentDB.setAttachmentId(attachment.getId());
					attachmentDB.setClaimId(claim.getId());
					attachmentDB.setDescription(attachment.getDescription());
					attachmentDB.setFileName(attachment.getFileName());
					attachmentDB.setFileType(attachment.getTypeCode());
					attachmentDB.setMD5Sum(attachment.getMd5());
					attachmentDB.setMimeType(attachment.getMimeType());
					attachmentDB.setPath("");
					attachmentDB.setStatus(AttachmentStatus._UPLOADED);
					attachmentDB.setSize(attachment.getSize());

					org.fao.sola.clients.android.opentenure.model.Attachment
							.createAttachment(attachmentDB);

					/*
					 * Here the creation of Folder for the claim
					 */

					FileSystemUtilities.createClaimantFolder(claimant.getId());
					FileSystemUtilities.createClaimFileSystem(downloadedClaim
							.getId());

				}

				List<Share> shares = downloadedClaim.getShares();

				for (Iterator iterator = shares.iterator(); iterator.hasNext();) {
					Share share = (Share) iterator.next();

					List<org.fao.sola.clients.android.opentenure.filesystem.json.model.Person> sharePersons = share
							.getOwners();

					for (Iterator iterator2 = sharePersons.iterator(); iterator2
							.hasNext();) {
						org.fao.sola.clients.android.opentenure.filesystem.json.model.Person person2 = (org.fao.sola.clients.android.opentenure.filesystem.json.model.Person) iterator2.next();

						Person personDB2 = new Person();

						personDB2.setContactPhoneNumber(person2
								.getPhone());
						
						
						Calendar cal = JsonUtilities
								.toCalendar(claimant.getBirthDate());
						birth = cal.getTime();

						if (birth != null)
							personDB2.setDateOfBirth(new java.sql.Date(birth.getTime()));
						else
							personDB2.setDateOfBirth(new java.sql.Date(2000, 2, 3));
						
						personDB2.setEmailAddress(person2.getEmail());
						personDB2.setFirstName(person2.getName());
						personDB2.setGender(person2.getGenderCode());
						personDB2.setLastName(person2.getLastName());
						personDB2.setMobilePhoneNumber(person2
								.getMobilePhone());
						personDB2.setPersonId(person2.getId());
						//personDB2.setPlaceOfBirth(person2.get);
						personDB2.setPostalAddress(person2.getAddress());

						Person.createPerson(personDB2);

					}
					
					Owner owner = new Owner(false);
					
					owner.setClaimId(downloadedClaim.getId());
					owner.setId(share.getId());
					owner.setPersonId(sharePersons.get(0).getId());
					owner.setOwnerId(sharePersons.get(0).getId());
					owner.setShares(share.getNominator());
					
					Owner.createOwner(owner);

				}

			}

			catch (Exception e) {
				Log.d("CommunityServerAPI", "ERROR SAVING DOWNLOADED  CLAIM "
						+ claim.getId());
				e.printStackTrace();
				success = false;
			}

		}

		return success;

	}

}
