package tanzent.cassette.bean.mp3;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:21
 */
public class PlayListSong {
    public int _Id;
    public int AudioId;
    //    public int AlbumID;
//    public String Album;
//    public int ArtistID;
//    public String Artist;
//    public String Data;
    public int PlayListID;
    public String PlayListName;

    public PlayListSong() {
    }

    public PlayListSong(int audioId, int playListID, String playListName) {
        AudioId = audioId;
        PlayListID = playListID;
        PlayListName = playListName;
    }
}
