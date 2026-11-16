package com.stymphiketreehighlighter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class StymphikeTreeHighlighterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(StymphikeTreeHighlighterPlugin.class);
		RuneLite.main(args);
	}
}