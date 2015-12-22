package ru.web24nsk.ltranslator.common;

public class helper
{
	public static String getMethodName(Object o, final int depth, String... args)
	{
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		String ret = o.getClass().getSimpleName() + '.' + ste[depth + 3].getMethodName();
		if (args.length > 0)
		{
			ret += ": ";
		}
		for (String s : args)
		{
			ret += s;
		}
		return ret;
	}

	public static String getOneLineText(String text, int max)
	{
		String str = "";
		char ch_prev = ' ';
		for (int i = 0; i < text.length(); i++)
		{
			if (i > max)
			{
				str += " ...";
				break;
			}
			char ch = text.charAt(i);
			switch (ch)
			{
				case '\n':
					ch = ' ';
					break;
			}
			if (ch_prev != ch || ch != ' ')
			{
				str += ch;
			}
			ch_prev = ch;
		}
		return str;
	}
}
