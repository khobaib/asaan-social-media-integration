package com.asaanloyalty.asaan.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.asaanloyalty.asaan.R;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import android.os.Bundle;
import android.os.Message;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class AuthMainActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener
{
	private static final Logger logger = Logger.getLogger(AuthMainActivity.class.getName());

	/* Request code used to invoke sign in user interactions. */
	private static final int RC_SIGN_IN = 0;
	private static final String TAG_ERROR_DIALOG = "plusClientFragmentErrorDialog";
	
	/* Client used to interact with Google APIs. */
	private GoogleApiClient mGoogleApiClient;
	/*
	 * Track whether the sign-in button has been clicked so that we know to
	 * resolve all issues preventing sign-in without waiting.
	 */
	private boolean mSignInClicked;

	/*
	 * Store the connection result from onConnectionFailed callbacks so that we
	 * can resolve them when the user clicks sign-in.
	 */
	private ConnectionResult mConnectionResult;
	/*
	 * A flag indicating that a PendingIntent is in progress and prevents us
	 * from starting further intents.
	 */
	private boolean mIntentInProgress;

	/** Facebook **/
	private UiLifecycleHelper mUiHelper;

	private ProgressDialog mConnectionProgressDialog = null;

	/**
	 * Handler for Facebook Session Status change
	 */
	private Session.StatusCallback mFacebookStatusCallback = new Session.StatusCallback()
	{
		@Override
		public void call(Session session, SessionState state, Exception exception)
		{
			onSessionStateChange(session, state, exception);
		}
	};

	/**
	 * Callback when the state of the Facebook Session changes.
	 * 
	 * @param session
	 *            the session which changed.
	 * @param state
	 *            the current state of the session.
	 * @param exception
	 *            any exception which occurred.
	 */
	private void onSessionStateChange(Session session, SessionState state, Exception exception)
	{
		if (state.isOpened())
		{
		}
		else if (state.isClosed())
		{
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		logger.log(Level.INFO, "onCreate started");
		setContentView(R.layout.activity_auth_main);

		mConnectionProgressDialog = new ProgressDialog(this);

		mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(Plus.API, null).addScope(Plus.SCOPE_PLUS_LOGIN).build();

		// Facebook Loginbutton setup
		mUiHelper = new UiLifecycleHelper(this, mFacebookStatusCallback);
		mUiHelper.onCreate(savedInstanceState);

		LoginButton authButton = (LoginButton) findViewById(R.id.authButton);
		authButton.setReadPermissions(Arrays.asList("email", "user_about_me"));

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				logger.log(Level.INFO, "G+ Signin button Onclick started");
				mSignInClicked = true;
				if (!mGoogleApiClient.isConnected())
				{
					mConnectionProgressDialog.setMessage("Signing in ...");
					mConnectionProgressDialog.show();
					if (mConnectionResult != null)
						resolveLastConnectionResultError(mConnectionResult);
					mGoogleApiClient.connect();
				} else
					onConnected(null);
			}
		});

		mConnectionProgressDialog.cancel();

		logger.log(Level.INFO, "onCreate completed");
	}

	@Override
	public void onBackPressed()
	{
		this.setResult(Activity.RESULT_FIRST_USER);
		super.onBackPressed();
	}

	private void doLogoff()
	{
		if (mGoogleApiClient.isConnected())
		{
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			mGoogleApiClient.connect();
		}
		Session session = Session.getActiveSession();
		if (session != null)
			session.closeAndClearTokenInformation();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		mGoogleApiClient.disconnect();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// For scenarios where the main activity is launched and user
		// session is not null, the session state change notification
		// may not be triggered. Trigger it if it's open/closed.
		Session session = Session.getActiveSession();
		if (session != null && (session.isOpened() || session.isClosed()))
		{
			onSessionStateChange(session, session.getState(), null);
		}

		mUiHelper.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		logger.log(Level.INFO, "Inside onActivityResult requestCode = " + requestCode + " resultCode = " + resultCode);
		if (requestCode == RC_SIGN_IN)
		{
			if (resultCode != RESULT_OK)
			{
				mSignInClicked = false;
			}

			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting())
			{
				mGoogleApiClient.connect();
			}
		}
		mUiHelper.onActivityResult(requestCode, resultCode, data);
		loginThroughFacebook();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		mUiHelper.onPause();
		if (mConnectionProgressDialog != null && mConnectionProgressDialog.isShowing() == true)
			mConnectionProgressDialog.cancel();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mUiHelper.onDestroy();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result)
	{
		if (!mIntentInProgress)
		{
			// Store the ConnectionResult so that we can use it later when the
			// user clicks
			// 'sign-in'.
			mConnectionResult = result;

			if (mSignInClicked)
			{
				// The user has already clicked 'sign-in' so we attempt to
				// resolve all
				// errors until the user is signed in, or they cancel.
				resolveLastConnectionResultError(mConnectionResult);
			}
		}
	}

	private void resolveLastConnectionResultError(ConnectionResult result)
	{
		if (mConnectionProgressDialog.isShowing())
		{
			// The user clicked the sign-in button already. Start to resolve
			// connection errors. Wait until onConnected() to dismiss the
			// connection dialog.
			if (GooglePlayServicesUtil.isUserRecoverableError(mConnectionResult.getErrorCode()))
			{
				// Show a dialog to install or enable Google Play services.
				showErrorDialog(ErrorDialogFragment.create(mConnectionResult.getErrorCode(), RC_SIGN_IN));
				return;
			}
			if (result.hasResolution())
			{
				try
				{
					logger.log(Level.INFO, "Inside onConnectionFailed hasResolution");
					result.startResolutionForResult(this, RC_SIGN_IN);
				} catch (SendIntentException e)
				{
					// The intent we had is not valid right now, perhaps the
					// remote
					// process died.
					// Try to reconnect to get a new resolution intent.
					mConnectionResult = null;
					mGoogleApiClient.connect();
				}
			} else
				Toast.makeText(this, "Google Plus Authentication Failed", Toast.LENGTH_LONG).show();
		}
	}

	private void showErrorDialog(DialogFragment errorDialog)
	{
		errorDialog.show(getFragmentManager(), TAG_ERROR_DIALOG);
	}

	public static final class ErrorDialogFragment extends GooglePlayServicesErrorDialogFragment
	{
		public static ErrorDialogFragment create(int errorCode, int requestCode)
		{
			ErrorDialogFragment fragment = new ErrorDialogFragment();
			fragment.setArguments(createArguments(errorCode, requestCode));
			return fragment;
		}
	}

	@Override
	public void onConnected(Bundle connectionHint)
	{
		// We've resolved any connection errors.
		mSignInClicked = false;
		mConnectionProgressDialog.dismiss();
		loginThroughGooglePlus();
	}

	private void loginThroughGooglePlus()
	{
		if (mGoogleApiClient.isConnected())
		{
			logger.log(Level.INFO, "Inside loginThroughGooglePlus connected mConnectionResult = " + mConnectionResult);
			mConnectionProgressDialog.setMessage("Signing in ...");
			mConnectionProgressDialog.show();

			String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
			Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
		    String personName = currentPerson.getDisplayName();
			logger.log(Level.INFO, "Inside loginThroughGooglePlus person available email = " + email + " name = "
					+ personName);
			Person.Image personImage = currentPerson.getImage();
		}
	}

	private void loginThroughFacebook()
	{
		final Session session = Session.getActiveSession();
		if (session.isOpened())
		{
			// Make an API call to get user data and define a
			// new callback to handle the response.
			mConnectionProgressDialog.setMessage("Signing in ...");
			mConnectionProgressDialog.show();

			Request request = Request.newMeRequest(session, new Request.GraphUserCallback()
			{
				@Override
				public void onCompleted(GraphUser user, Response response)
				{
					// If the response is successful
					if (session == Session.getActiveSession())
					{
						if (user != null)
						{
							// Set the id for the ProfilePictureView view that
							// in turn displays the profile picture.
							// profilePictureView.setProfileId(user.getId());
							// Set the Textview's text to the user's name.
							String facebookProfilePhotoURL = "http://graph.facebook.com/" + user.getId()
									+ "/picture?type=small";

							String name = user.getName();
							String email = (String) user.getProperty("email");
							String photoUrl = facebookProfilePhotoURL;
						}
					}
					if (response.getError() != null)
					{
						mConnectionProgressDialog.dismiss();
						Toast.makeText(AuthMainActivity.this,
								"Facebook Authentication Failed - " + response.getError().getErrorMessage(),
								Toast.LENGTH_LONG).show();
					}
				}
			});
			request.executeAsync();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		mUiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onConnectionSuspended(int cause)
	{
		mGoogleApiClient.connect();
	}
}
