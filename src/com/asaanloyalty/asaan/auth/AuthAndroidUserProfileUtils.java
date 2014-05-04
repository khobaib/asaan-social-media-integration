package com.asaanloyalty.asaan.auth;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Patterns;

import com.google.api.client.util.Lists;

public class AuthAndroidUserProfileUtils
{
	/**
	 * Interface for interacting with the result of
	 * {@link AccountUtils#getUserProfile}.
	 * 
	 * @param <_possible_emails>
	 */
	private static final Logger logger = Logger.getLogger(AuthAndroidUserProfileUtils.class.getName());

	public static class AndroidContactProfile
	{

		/** The primary email address */
		private String _primary_email;
		/** The primary name */
		private String _primary_name;
		/** The primary phone number */
		private String _primary_phone_number;
		/** A list of possible email addresses for the user */
		private List<String> _possible_emails = Lists.newArrayList();
		/** A list of possible names for the user */
		private List<String> _possible_names = Lists.newArrayList();
		/** A list of possible phone numbers for the user */
		private List<String> _possible_phone_numbers = Lists.newArrayList();
		/** A possible photo for the user */
		private Uri _possible_photo;

		/**
		 * Adds an email address to the list of possible email addresses for the
		 * user. Retains information about whether this email address is the
		 * primary email address of the user.
		 * 
		 * @param email
		 *            the possible email address
		 * @param is_primary
		 *            whether the email address is the primary email address
		 */
		public void addPossibleEmail(String email, boolean is_primary)
		{
			if (email == null)
				return;
			if (is_primary)
			{
				_primary_email = email;
				_possible_emails.add(email);
			} else
				_possible_emails.add(email);
		}

		/**
		 * Adds a name to the list of possible names for the user.
		 * 
		 * @param name
		 *            the possible name
		 */
		public void addPossibleName(String name)
		{
			if (name != null)
				_possible_names.add(name);
		}

		/**
		 * Adds a phone number to the list of possible phone numbers for the
		 * user. Retains information about whether this phone number is the
		 * primary phone number of the user.
		 * 
		 * @param phone_number
		 *            the possible phone number
		 * @param is_primary
		 *            whether the phone number is the primary phone number
		 */
		public void addPossiblePhoneNumber(String phone_number, boolean is_primary)
		{
			if (phone_number == null)
				return;
			if (is_primary)
			{
				_primary_phone_number = phone_number;
				_possible_phone_numbers.add(phone_number);
			} else
				_possible_phone_numbers.add(phone_number);
		}

		/**
		 * Sets the possible photo for the user.
		 * 
		 * @param photo
		 *            the possible photo
		 */
		public void addPossiblePhoto(Uri photo)
		{
			if (photo != null)
				_possible_photo = photo;
		}

		/**
		 * Retrieves the possible photo.
		 * 
		 * @return the possible photo
		 */
		public String possiblePhotoPath()
		{
			if (_possible_photo != null)
				return _possible_photo.getEncodedPath();
			else
				return null;
		}

		/**
		 * Retrieves the primary email address.
		 * 
		 * @return the primary email address
		 */
		public String primaryEmail()
		{
			if (!TextUtils.isEmpty(_primary_email))
				return _primary_email;
			else if (_possible_emails.size() > 0)
				return _possible_emails.get(0);
			return null;
		}

		/**
		 * Retrieves the primary phone number
		 * 
		 * @return the primary phone number
		 */
		public String primaryPhoneNumber()
		{
			if (!TextUtils.isEmpty(_primary_phone_number))
				return _primary_phone_number;
			else if (_possible_phone_numbers.size() > 0)
				return _possible_phone_numbers.get(0);
			return null;
		}

		/**
		 * Retrieves the primary name
		 * 
		 * @return the primary phone number
		 */
		public String primaryName()
		{
			if (!TextUtils.isEmpty(_primary_name))
				return _primary_name;
			else if (_possible_names.size() > 0)
				return _possible_names.get(0);
			return null;
		}
	}

	/**
	 * Retrieves the user profile information.
	 * 
	 * @param context
	 *            the context from which to retrieve the user profile
	 * @return the user profile
	 */
	public static AndroidContactProfile getUserProfile(Context context)
	{
		AndroidContactProfile contactProfile = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			contactProfile = getUserProfileOnIcsDevice(context);

		if (!TextUtils.isEmpty(contactProfile.primaryEmail()))
			return contactProfile;
		else
			return getUserProfileOnGingerbreadDevice(context);
	}

	/**
	 * Retrieves the user profile information in a manner supported by
	 * Gingerbread devices.
	 * 
	 * @param context
	 *            the context from which to retrieve the user's email address
	 *            and name
	 * @return a list of the possible user's email address and name
	 */
	private static AndroidContactProfile getUserProfileOnGingerbreadDevice(Context context)
	{
		try
		{
			// Other that using Patterns (API level 8) this works on devices
			// down to
			// API level 5
			final Matcher valid_email_address = Patterns.EMAIL_ADDRESS.matcher("");
			// final Account[] accounts =
			// AccountManager.get(context).getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
			final Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
			final AndroidContactProfile user_profile = new AndroidContactProfile();
			// As far as I can tell, there is no way to get the real name or
			// phone
			// number from the Google account
			for (Account account : accounts)
			{
				if (valid_email_address.reset(account.name).matches())
					user_profile.addPossibleEmail(account.name, true);
			}
			// Gets the phone number of the device is the device has one
			if (context.getPackageManager().hasSystemFeature(Context.TELEPHONY_SERVICE))
			{
				final TelephonyManager telephony = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
				if (!TextUtils.isEmpty(telephony.getLine1Number()))
					user_profile.addPossiblePhoneNumber(telephony.getLine1Number(), true);
			}

			return user_profile;
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "getUserProfileOnIcsDevice Failed - " + e.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves the user profile information in a manner supported by Ice Cream
	 * Sandwich devices.
	 * 
	 * @param context
	 *            the context from which to retrieve the user's email address
	 *            and name
	 * @return a list of the possible user's email address and name
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private static AndroidContactProfile getUserProfileOnIcsDevice(Context context)
	{
		try
		{
			final ContentResolver content = context.getContentResolver();
			final Cursor cursor = content.query(
			// Retrieves data rows for the device user's 'profile' contact
					Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
							ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

					// Selects only email addresses or names
					ContactsContract.Contacts.Data.MIMETYPE + "=? OR " + ContactsContract.Contacts.Data.MIMETYPE
							+ "=? OR " + ContactsContract.Contacts.Data.MIMETYPE + "=? OR "
							+ ContactsContract.Contacts.Data.MIMETYPE + "=?", new String[]{
							ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
							ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
							ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
							ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE},

					// Show primary rows first. Note that there won't be a
					// primary
					// email address if the
					// user hasn't specified one.
					ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");

			final AndroidContactProfile user_profile = new AndroidContactProfile();
			String mime_type;
			while (cursor.moveToNext())
			{
				mime_type = cursor.getString(ProfileQuery.MIME_TYPE);
				if (mime_type.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE))
				{
					String email = cursor.getString(ProfileQuery.EMAIL);
					if (!TextUtils.isEmpty(email))
						user_profile.addPossibleEmail(email, cursor.getInt(ProfileQuery.IS_PRIMARY_EMAIL) > 0);
				} else if (mime_type.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
				{
					String givenName = cursor.getString(ProfileQuery.GIVEN_NAME);
					String familyName = cursor.getString(ProfileQuery.FAMILY_NAME);
					if (!TextUtils.isEmpty(givenName) && !TextUtils.isEmpty(familyName))
						user_profile.addPossibleName(givenName + " " + familyName);
				} else if (mime_type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
				{
					String email = cursor.getString(ProfileQuery.PHONE_NUMBER);
					if (!TextUtils.isEmpty(email))
						user_profile.addPossiblePhoneNumber(email,
								cursor.getInt(ProfileQuery.IS_PRIMARY_PHONE_NUMBER) > 0);
				} else if (mime_type.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
				{
					String photo = cursor.getString(ProfileQuery.PHOTO);
					if (!TextUtils.isEmpty(photo))
						user_profile.addPossiblePhoto(Uri.parse(photo));
				}
			}

			cursor.close();

			return user_profile;
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "getUserProfileOnIcsDevice Failed - " + e.getMessage());
		}
		return null;
	}

	/**
	 * Contacts user profile query interface.
	 */
	private interface ProfileQuery
	{
		/** The set of columns to extract from the profile query results */
		String[] PROJECTION = {ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
				ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
				ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
				ContactsContract.CommonDataKinds.Photo.PHOTO_URI, ContactsContract.Contacts.Data.MIMETYPE};

		/** Column index for the email address in the profile query results */
		int EMAIL = 0;
		/**
		 * Column index for the primary email address indicator in the profile
		 * query results
		 */
		int IS_PRIMARY_EMAIL = 1;
		/** Column index for the family name in the profile query results */
		int FAMILY_NAME = 2;
		/** Column index for the given name in the profile query results */
		int GIVEN_NAME = 3;
		/** Column index for the phone number in the profile query results */
		int PHONE_NUMBER = 4;
		/**
		 * Column index for the primary phone number in the profile query
		 * results
		 */
		int IS_PRIMARY_PHONE_NUMBER = 5;
		/** Column index for the photo in the profile query results */
		int PHOTO = 6;
		/** Column index for the MIME type in the profile query results */
		int MIME_TYPE = 7;
	}
}
