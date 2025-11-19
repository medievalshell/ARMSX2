package kr.co.iefriends.pcsx2.data.model;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
public class GameEntry implements Parcelable {
    public final String title;
    public final Uri uri;
    public String serial;
    public String gameTitle;
    public GameEntry(String t, Uri u) {
        title = t;
        uri = u;
    }
    protected GameEntry(Parcel in) {
        title = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
        serial = in.readString();
        gameTitle = in.readString();
    }
    public static final Creator<GameEntry> CREATOR = new Creator<GameEntry>() {
        @Override
        public GameEntry createFromParcel(Parcel in) {
            return new GameEntry(in);
        }
        @Override
        public GameEntry[] newArray(int size) {
            return new GameEntry[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeParcelable(uri, flags);
        dest.writeString(serial);
        dest.writeString(gameTitle);
    }
    public String fileTitleNoExt() {
        int i = title.lastIndexOf('.');
        return (i > 0) ? title.substring(0, i) : title;
    }
}