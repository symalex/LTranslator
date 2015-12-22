package ru.web24nsk.ltranslator.ui.fragments;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import ru.web24nsk.ltranslator.MainApp;
import ru.web24nsk.ltranslator.R;
import ru.web24nsk.ltranslator.data.DataProvider;
import ru.web24nsk.ltranslator.data.SettingsProvider;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData;
import ru.web24nsk.ltranslator.ui.activities.MainActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsFragment extends Fragment implements OnCheckedChangeListener, OnItemSelectedListener
{
	public static final String FTAG = "settings_fragment";

	@Bind(R.id.fragment_settings_api_key)
	protected EditText mApiKeyText;
	@Bind(R.id.fragment_settings_autodetect)
	protected CheckBox mCheckboxAutoDetect;
	@Bind(R.id.fragment_settings_src_lang_info)
	protected TextView mSourceLanguageInfo;
	@Bind(R.id.fragment_settings_src_lang)
	protected Spinner mSpinnerSourceLanguage;
	@Bind(R.id.fragment_settings_btn_swap)
	protected Button mBtnSwap;
	@Bind(R.id.fragment_settings_result_lang)
	protected Spinner mSpinnerResultLanguage;
	@Bind(R.id.fragment_settings_btn_ok)
	protected Button mBtnOk;
	@Bind(R.id.fragment_settings_btn_cancel)
	protected Button mBtnCancel;

	private String mDirection;
	private DataProvider mProvider;
	private SettingsProvider mSettings;
	private YandexTranslateAPIData mAPIData;

	private String mInitialApiKey, mInitialDirection;

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
			updateChanging();
		}
	};

	public static Fragment newInstance()
	{
		return new SettingsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_settings, container, false);
		ButterKnife.bind(this, view);

		mProvider = ((MainApp) getContext().getApplicationContext()).getDataProvider();
		mSettings = mProvider.getSettings();
		mAPIData = mSettings.getTranslateAPIData();

		// save initial settings
		mInitialApiKey = mAPIData.getApiKey();
		mDirection = mInitialDirection = mAPIData.getTranslateDirection();
		updateInitialSourceLang(mDirection);

		mApiKeyText.setText(mAPIData.getApiKey());
		mApiKeyText.addTextChangedListener(mTextWatcherTop);

		mCheckboxAutoDetect.setOnCheckedChangeListener(this);
		mSpinnerSourceLanguage.setOnItemSelectedListener(this);
		mSpinnerResultLanguage.setOnItemSelectedListener(this);

		mCheckboxAutoDetect.setChecked(mAPIData.getTranslateDirection().length() <= 2);
		setSpinnersDropDownLists(mAPIData.getTranslateDirection());
		setHasOptionsMenu(true);

		updateChanging();

		return view;
	}

	private void CloseSettings()
	{
		Activity activity = getActivity();
		if (activity instanceof MainActivity)
		{
			((MainActivity) activity).navigateFragment(MainActivity.FragmentPage.MAIN_FRAGMENT);
		}
	}

	private boolean isDataChanged()
	{
		return !(
				mInitialApiKey.equals(mApiKeyText.getText().toString().trim()) && mInitialDirection.equals(mDirection)
		);
	}

	private void updateChanging()
	{
		mBtnOk.setEnabled(isDataChanged());
	}

	private void updateInitialSourceLang(String dir)
	{
		//mLastSourceLang = YandexTranslateAPIData.src(dir);
	}

	@OnClick(R.id.fragment_settings_btn_ok)
	public void onButtonClickOk(View view)
	{
		if (isDataChanged())
		{
			mAPIData.setApiKey(mApiKeyText.getText().toString().trim());
			mAPIData.setTranslateDirection(mDirection);
			mProvider.getTranslateAPI().setApiKey(mApiKeyText.getText().toString().trim());
			mProvider.getTranslateAPI().setTranslateDirection(mDirection);
			mAPIData.setDisableComparatorUpdateOnce(true);
			mProvider.setForceTextTranslateFlag(true);
			//mAPIData.setRequiredSaveHistory(true);
		}
		CloseSettings();
	}

	@OnClick(R.id.fragment_settings_btn_cancel)
	public void onButtonClickCancel(View view)
	{
		CloseSettings();
	}

	@OnClick(R.id.fragment_settings_btn_swap)
	public void onButtonClickSwap(View view)
	{
		if (!mCheckboxAutoDetect.isChecked())
		{
			setSpinnersDropDownLists(YandexTranslateAPIData.direction(get_destination_spinner_selected_lang(), get_source_spinner_selected_lang()));
			updateChanging();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if (isChecked)
		{
			setSpinnersDropDownLists(get_destination_spinner_selected_lang());
		}
		else
		{
			//setSpinnersDropDownLists(YandexTranslateAPIData.direction(mLastSourceLang, get_destination_spinner_selected_lang()));
			setSpinnersDropDownLists(YandexTranslateAPIData.direction(get_source_spinner_selected_lang(), get_destination_spinner_selected_lang()));
		}
		updateChanging();
	}

	private String get_source_spinner_selected_lang()
	{
		return mAPIData.encode((String) mSpinnerSourceLanguage.getSelectedItem(), false);
	}

	private String get_destination_spinner_selected_lang()
	{
		return mAPIData.encode((String) mSpinnerResultLanguage.getSelectedItem(), true);
	}

	private boolean select_spinner_by_name(Spinner spinner, String array[], String name)
	{
		boolean ok = false;
		// find selection
		int i;
		for (i = 0; i < array.length; i++)
		{
			if (array[i].equals(name))
			{
				break;
			}
		}

		if (i >= 0 && i < array.length)
		{
			spinner.setSelection(i);
			ok = true;
		}
		return ok;
	}

	private void update_source_language_drop_down_spinner(String direction)
	{
		// get available sources
		Set<String> src_set = new HashSet<>();
		for (String dir : mSettings.getTranslateAPIData().getDirs())
		{
			src_set.add(mSettings.getTranslateAPIData().getLangs().get(YandexTranslateAPIData.src(dir)));
		}
		String sources[] = src_set.toArray(new String[src_set.size()]);
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sources);
		mSpinnerSourceLanguage.setAdapter(spinnerArrayAdapter);

		String src_name = mSettings.getTranslateAPIData().getLangs().get(YandexTranslateAPIData.src(direction));
		if (sources.length > 0 && !select_spinner_by_name(mSpinnerSourceLanguage, sources, src_name))
		{
			View view = getView();
			if (view != null)
			{
				Snackbar.make(view, String.format(getResources().getString(R.string.msg_settings_source_lang_not_supported), src_name), Snackbar.LENGTH_LONG).show();
			}
			mSpinnerSourceLanguage.setSelection(0);
		}
	}

	private void update_destination_language_drop_down_spinner(String direction)
	{
		// get available destinations for [direction]
		Set<String> dest_set = new HashSet<>();
		for (String dir : mSettings.getTranslateAPIData().getDirs())
		{
			if (YandexTranslateAPIData.src(dir).equals(YandexTranslateAPIData.src(direction)))
			{
				dest_set.add(mSettings.getTranslateAPIData().getLangs().get(YandexTranslateAPIData.dest(dir)));
			}
		}
		String destinations[] = dest_set.toArray(new String[dest_set.size()]);
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, destinations);
		mSpinnerResultLanguage.setAdapter(spinnerArrayAdapter);

		String dst_name = mSettings.getTranslateAPIData().getLangs().get(YandexTranslateAPIData.dest(direction));
		if (destinations.length > 0 && !select_spinner_by_name(mSpinnerResultLanguage, destinations, dst_name))
		{
			View view = getView();
			if (view != null)
			{
				Snackbar.make(view, String.format(getResources().getString(R.string.msg_settings_destination_lang_not_supported), dst_name), Snackbar.LENGTH_LONG).show();
			}
			mSpinnerResultLanguage.setSelection(0);
		}
	}

	private void setSpinnersDropDownLists(String direction)
	{
		if (mDirection == null)
		{
			return;
		}
		if (direction.length() > 2)
		{
			mBtnSwap.setVisibility(View.VISIBLE);
			mSourceLanguageInfo.setVisibility(View.VISIBLE);
			mSpinnerSourceLanguage.setVisibility(View.VISIBLE);

			update_source_language_drop_down_spinner(direction);
			update_destination_language_drop_down_spinner(direction);
			updateInitialSourceLang(direction);

			mDirection = direction;
		}
		else if (direction.length() == 2)
		{
			// autodetect source language
			mBtnSwap.setVisibility(View.GONE);
			mSourceLanguageInfo.setVisibility(View.GONE);
			mSpinnerSourceLanguage.setVisibility(View.GONE);

			Set<String> dest_set = new HashSet<>();
			for (String dir : mSettings.getTranslateAPIData().getDirs())
			{
				dest_set.add(mSettings.getTranslateAPIData().getLangs().get(YandexTranslateAPIData.dest(dir)));
			}
			String destinations[] = dest_set.toArray(new String[dest_set.size()]);
			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, destinations);
			mSpinnerResultLanguage.setAdapter(spinnerArrayAdapter);

			select_spinner_by_name(mSpinnerResultLanguage, destinations, mSettings.getTranslateAPIData().getLangs().get(direction));

			mDirection = direction;
		}
		else
		{
			// fail save
			setSpinnersDropDownLists(YandexTranslateAPIData.DEFAULT_DEST_VALUE);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		switch (parent.getId())
		{
			case R.id.fragment_settings_src_lang:
			case R.id.fragment_settings_result_lang:
				if (mCheckboxAutoDetect.isChecked())
				{
					setSpinnersDropDownLists(get_destination_spinner_selected_lang());
				}
				else
				{
					setSpinnersDropDownLists(YandexTranslateAPIData.direction(get_source_spinner_selected_lang(), get_destination_spinner_selected_lang()));
				}
				updateChanging();
				break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		menu.findItem(R.id.action_settings).setVisible(false);
	}

}
