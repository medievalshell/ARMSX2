package kr.co.iefriends.pcsx2.viewmodels;

import android.text.TextUtils;
import android.content.ContentResolver;

import java.util.List;

import kr.co.iefriends.pcsx2.data.model.GameEntry;
import kr.co.iefriends.pcsx2.data.repositories.RedumpDB;

public class GameListViewModel {

    public static void resolveMetadataForEntries(ContentResolver cr, List<GameEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (GameEntry ge : entries) {
            if (ge == null || ge.uri == null) {
                continue;
            }
            try {
                boolean needsSerial = TextUtils.isEmpty(ge.serial);
                boolean needsTitle = TextUtils.isEmpty(ge.gameTitle);
                if (!needsSerial && !needsTitle) {
                    continue;
                }
                RedumpDB.Result rd = RedumpDB.lookupByFile(cr, ge.uri);
                if (rd != null) {
                    if (needsSerial && !TextUtils.isEmpty(rd.serial)) {
                        ge.serial = rd.serial;
                    }
                    if (needsTitle && !TextUtils.isEmpty(rd.name)) {
                        ge.gameTitle = rd.name;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

}