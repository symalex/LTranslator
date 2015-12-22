package ru.web24nsk.ltranslator.ui.fragments;

import java.util.Map;
import java.util.Set;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.web24nsk.ltranslator.MainApp;
import ru.web24nsk.ltranslator.R;
import ru.web24nsk.ltranslator.common.helper;
import ru.web24nsk.ltranslator.data.DataProvider;
import ru.web24nsk.ltranslator.data.DataProvider.DataProviderListener;
import ru.web24nsk.ltranslator.network.InternetReceiver;
import ru.web24nsk.ltranslator.network.InternetReceiver.InternetReceiverListener;
import ru.web24nsk.ltranslator.network.YandexTranslateAPI;
import ru.web24nsk.ltranslator.network.YandexTranslateAPI.YandexTranslateApiListener;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainFragment extends Fragment implements InternetReceiverListener, YandexTranslateApiListener, DataProviderListener
{
	private static final String TAG = "MainFragment";
	public static final String FTAG = "main_fragment";

	@Bind(R.id.content_main_layout_translate)
	protected LinearLayout mLayoutTranslate;
	@Bind(R.id.content_main_error_state_text)
	protected TextView mErrorText;
	@Bind(R.id.fragment_main_top_text_info)
	protected TextView mTextViewTopTextInfo;
	@Bind(R.id.content_main_edit_text_top)
	protected EditText mEditTextTop;
	@Bind(R.id.fragment_main_bottom_text_info)
	protected TextView mTextViewBottomTextInfo;
	@Bind(R.id.content_main_edit_text_bottom)
	protected EditText mEditTextBottom;
	@Bind(R.id.fragment_main_yandex_text_ref)
	protected TextView mYandexRef;

	// set from MainActivity
	private FloatingActionButton mBtnTranslate;
	private boolean mInternalTextUpdateFlag = false;

	private int mWordsCount = 0;
	private String mTopTextComparatorStr = "";
	private int mTopTextPoolPeriodInMs;

	private InternetReceiver mReceiver;
	private DataProvider mDataProvider;
	private YandexTranslateAPIData mAPIData;
	private Menu mMenu;
	private ClipboardManager mClipboard;

	private final Handler mHandler = new Handler();

	public void setBtnTranslate(FloatingActionButton btnTranslate)
	{
		mBtnTranslate = btnTranslate;
	}

	public static Fragment newInstance()
	{
		return new MainFragment();
	}

	private final Runnable mRunnableTextTop = new Runnable()
	{
		@Override
		public void run()
		{
			doTranslateText();
		}
	};

	private final TextWatcher mTextWatcherTop = new TextWatcher()
	{
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			if (!mInternalTextUpdateFlag || mDataProvider.isForceTextTranslateFlag())
			{
				mHandler.removeCallbacks(mRunnableTextTop);
				mDataProvider.setForceTextTranslateFlag(false);
				if (mEditTextTop.getText().toString().trim().length() > 0)
				{
					mHandler.postDelayed(mRunnableTextTop, mTopTextPoolPeriodInMs);
					mAPIData.setCurrentTextInHistory(false);
				}
				else
				{
					mAPIData.setCurrentTextInHistory(true);
					if (mEditTextBottom != null)
					{
						mEditTextBottom.setText("");
						updateApiDataSettingsText();
						updateTextComparators();
						updateInfoTexts();
					}
				}
				updateTranslateButtonHistoryAccess();
				updateMenu();
			}
			else
			{
				mInternalTextUpdateFlag = false;
			}
		}
	};

	private String getTextComparator(String text)
	{
		return text.replaceAll(" ", "").replaceAll("\n", "");
	}

	private int wordsCount(String s)
	{
		int cnt = 0;
		s = s.trim();
		if (s.length() > 0)
		{
			cnt = s.split("\\w+").length;
			if (cnt == 0)
			{
				cnt = 1;
			}
		}

		return cnt;
	}

	private void updateTextComparators()
	{
		if (mAPIData != null)
		{
			if (!mAPIData.isDisableComparatorUpdateOnce())
			{
				mWordsCount = wordsCount(mEditTextTop.getText().toString());
				mTopTextComparatorStr = getTextComparator(mEditTextTop.getText().toString());
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		ButterKnife.bind(this, view);

		mClipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		mDataProvider = ((MainApp) getContext().getApplicationContext()).getDataProvider();
		mAPIData = mDataProvider.getSettings().getTranslateAPIData();
		mReceiver = mDataProvider.getInternetReceiver();
		mDataProvider.addDataProviderListener(this);

		mTopTextPoolPeriodInMs = getContext().getResources().getInteger(R.integer.auto_translate_timeout);

		updateTranslateButtonHistoryAccess();

		updateViewData();
		mEditTextTop.addTextChangedListener(mTextWatcherTop);

		onInternetConnectionChange(mReceiver);

		setHasOptionsMenu(true);

		mYandexRef.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String url = getResources().getString(R.string.yandex_translate_api_url);
				Intent browser = new Intent(Intent.ACTION_VIEW);
				browser.setData(Uri.parse(url));
				startActivity(browser);
			}
		});

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.clipboard_menu, menu);

		mMenu = menu;
		updateMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_clear_text:
				if (mEditTextTop != null)
				{
					mEditTextTop.setText("");
				}
				return true;

			case R.id.action_clipboard_copy:
				if (mEditTextTop != null)
				{
					ClipData clip = ClipData.newPlainText("label", mEditTextTop.getText());
					mClipboard.setPrimaryClip(clip);
				}
				return true;

			case R.id.action_clipboard_copy_translation:
				if (mEditTextBottom != null)
				{
					ClipData clip = ClipData.newPlainText("label", mEditTextBottom.getText());
					mClipboard.setPrimaryClip(clip);
				}
				return true;

			case R.id.action_clipboard_paste:
				if (mEditTextTop != null)
				{
					if (mClipboard.hasPrimaryClip())
					{
						ClipData.Item cdata = mClipboard.getPrimaryClip().getItemAt(0);
						mEditTextTop.setText(cdata.getText());
					}
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void updateMenu()
	{
		if (mMenu != null && mClipboard != null && mEditTextTop != null && mEditTextBottom != null)
		{
			mMenu.findItem(R.id.action_clear_text).setVisible(mEditTextTop.getText().toString().trim().length() > 0);
			mMenu.findItem(R.id.action_clipboard_copy).setVisible(mEditTextTop.getText().toString().trim().length() > 0);
			mMenu.findItem(R.id.action_clipboard_copy_translation).setVisible(mEditTextBottom.getText().toString().trim().length() > 0);
			mMenu.findItem(R.id.action_clipboard_paste).setVisible(mClipboard.hasPrimaryClip());
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();

		if (mDataProvider != null)
		{
			mDataProvider.addDataProviderListener(this);
			mDataProvider.getInternetReceiver().addInternetReceiverListener(this);
			mDataProvider.getTranslateAPI().addApiListener(this);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		updateViewData();
		updateInfoTexts();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		updateApiDataSettingsText();
	}

	private void updateApiDataSettingsText()
	{
		if (mAPIData != null)
		{
			mAPIData.setSrcText(mEditTextTop.getText().toString().trim());
			mAPIData.setDestText(mEditTextBottom.getText().toString().trim());
			if (mEditTextTop.getText().toString().trim().length() == 0)
			{
				mAPIData.setDetectedDirection("");
			}
		}
	}

	@Override
	public void onStop()
	{
		if (mDataProvider != null)
		{
			mDataProvider.getTranslateAPI().removeApiListener(this);
			mDataProvider.getInternetReceiver().removeInternetReceiverListener(this);
			mDataProvider.removeDataProviderListener(this);
		}

		super.onStop();
	}

	@Override
	public void onLoadDataComplete()
	{
		updateInfoTexts();
	}

	private void updateTranslateButtonHistoryAccess()
	{
		if (mBtnTranslate != null && mAPIData != null)
		{
			if (mAPIData.isCurrentTextInHistory())
			{
				mBtnTranslate.setVisibility(View.GONE);
			}
			else
			{
				mBtnTranslate.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setTopText(String src_text)
	{
		if (mEditTextTop != null)
		{
			mInternalTextUpdateFlag = true;
			mEditTextTop.setText(src_text);
		}
	}

	private void setBottomText(String dest_text)
	{
		if (mEditTextBottom != null)
		{
			mEditTextBottom.setText(dest_text);
		}
	}

	private void updateViewData()
	{
		setTopText(mAPIData.getSrcText());
		setBottomText(mAPIData.getDestText());
		updateTextComparators();
		updateTranslateButtonHistoryAccess();
		updateMenu();
	}

	private void updateInfoTexts()
	{
		updateTranslateButtonHistoryAccess();
		if (mTextViewTopTextInfo != null && mTextViewBottomTextInfo != null)
		{
			String direction = mAPIData.getTranslateDirection();
			setSourceTextInfo(direction, mAPIData.decode(YandexTranslateAPIData.src(direction), false));
			setDestinationTextInfo(direction, mAPIData.decode(YandexTranslateAPIData.dest(direction), true));
		}
		if (mReceiver != null)
		{
			onInternetConnectionChange(mReceiver);
		}
		updateMenu();
	}

	private boolean isTopTextChanged()
	{
		String top = mEditTextTop.getText().toString().trim();
		return top.length() > 0 && !(mWordsCount == wordsCount(top) && mTopTextComparatorStr.equals(getTextComparator(mEditTextTop.getText().toString())));
	}

	private void doTranslateText()
	{
		if (isTopTextChanged())
		{
			updateTextComparators();
			mDataProvider.getTranslateAPI().translate(mEditTextTop.getText().toString().trim());
		}
	}

	private void doSaveHistory(String src_text, String dest_text)
	{
		if (mAPIData != null && !mAPIData.isCurrentTextInHistory() && mAPIData.isRequiredSaveHistory() &&
				mDataProvider != null && !mDataProvider.getTranslateAPI().isServiceBusy() && src_text.length() > 0 && dest_text.length() > 0)
		{
			mAPIData.setRequiredSaveHistory(false);

			// save history data
			mDataProvider.getLocalDataBase().addToHistory(
					mDataProvider.getTranslateAPI().getTranslateDirection(),
					src_text, dest_text,
					mDataProvider.getTranslateAPI().getDetectedDirection()
			);
			mAPIData.setCurrentTextInHistory(true);
		}
		updateTranslateButtonHistoryAccess();
	}

	private String change_text(String s)
	{
		String ui_lang = mDataProvider.getTranslateAPI().getUiLang();
		if (ui_lang.equals("ru"))
		{
			String ret = s.substring(0, s.length() - 2);
			return ret + "ом";
		}
		return s;
	}

	public void setSourceTextInfo(String dir, String lang_name)
	{
		if (mTextViewTopTextInfo != null && dir != null && lang_name != null)
		{
			if (YandexTranslateAPIData.src(dir).equals(YandexTranslateAPIData.dest(dir)))
			{
				if (mEditTextTop != null && mEditTextTop.getText().toString().trim().length() > 0)
				{
					if (mAPIData != null && mAPIData.getDetectedDirection().length() > 0)
					{
						mTextViewTopTextInfo.setText(String.format(getResources().getString(R.string.msg_main_source_lang_detected_as),
								mAPIData.decode(YandexTranslateAPIData.src(mAPIData.getDetectedDirection()), false)));
					}
					else
					{
						mTextViewTopTextInfo.setText(getResources().getString(R.string.msg_main_source_unknown_language));
					}
				}
				else
				{
					mTextViewTopTextInfo.setText(getResources().getString(R.string.msg_main_source_enter_text));
				}
			}
			else
			{
				mTextViewTopTextInfo.setText(String.format(getResources().getString(R.string.msg_main_source_text_info), change_text(lang_name)));
			}
		}
	}

	public void setDestinationTextInfo(String dir, String lang_name)
	{
		if (mTextViewBottomTextInfo != null && lang_name != null)
		{
			mTextViewBottomTextInfo.setText(String.format(getResources().getString(R.string.msg_main_destination_text_info), change_text(lang_name)));
		}
	}

	private void setEnabledUiElements(boolean enabled)
	{
		mLayoutTranslate.setEnabled(enabled);
		mTextViewTopTextInfo.setEnabled(enabled);
		mTextViewBottomTextInfo.setEnabled(enabled);
		mEditTextTop.setEnabled(enabled);
		mEditTextBottom.setEnabled(enabled);
	}

	@Override
	public void onInternetConnectionChange(InternetReceiver receiver)
	{
		if (mLayoutTranslate != null && mErrorText != null && mDataProvider != null)
		{
			setEnabledUiElements(receiver.isConnectionOk());
			mErrorText.setVisibility(View.GONE);
			mLayoutTranslate.setVisibility(View.VISIBLE);
			if (!receiver.isConnectionOk())
			{
				if (mDataProvider.getHistoryList().size() == 0)
				{
					mErrorText.setVisibility(View.VISIBLE);
					mLayoutTranslate.setVisibility(View.GONE);
				}
			}
		}
	}

	public void onButtonClickTranslate(View view)
	{
		mHandler.removeCallbacks(mRunnableTextTop);
		if (mDataProvider != null && mAPIData != null)
		{
			if (!mAPIData.isCurrentTextInHistory())
			{
				mAPIData.setRequiredSaveHistory(true);
			}

			if (isTopTextChanged())
			{
				doTranslateText();
			}
			else
			{
				doSaveHistory(mDataProvider.getTranslateAPI().getLastSourceText(), mDataProvider.getTranslateAPI().getLastResultText());
			}
		}
	}

	@Override
	public void onSupportedLangsUpdate(YandexTranslateAPI api, Set<String> dirs, Map<String, String> langs)
	{
	}

	@Override
	public void onDetectedLangUpdate(YandexTranslateAPI api, String detected_lang)
	{
	}

	@Override
	public void onTranslationUpdate(YandexTranslateAPI api, String detected_lang, String detected_dir, String text)
	{
		if (mEditTextBottom != null)
		{
			mEditTextBottom.setText(text);
			updateInfoTexts();
		}
	}

	@Override
	public void onHttpRequestResultError(YandexTranslateAPI api, int http_status_code, String message)
	{
	}

}
