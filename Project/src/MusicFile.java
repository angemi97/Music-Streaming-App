import java.io.Serializable;

/* MusicFile objects are used to store the Mp3File's information.
 * The strings trackName, artistName, albumInfo, genre are extracted from 
 * the Id3v1Tag of the mp3 file that the consumer wants to receive.
 * The byte[] musicFileExtract is extracted from the mp3 file itself.
 */
public class MusicFile implements Serializable{
	private static final long serialVersionUID = 1L;
	private String trackName; 
	private String artistName; 
	private String albumInfo;
	private String genre;
	private byte[] musicFileExtract; //The chunk of data that is going to be sent from the Publisher -> Broker -> Consumer.
	
	 //Class constructor.
	
	public MusicFile(String trackName, String artistName, String albumInfo, String genre, byte[] musicFileExtract) {
		if(trackName != null) {
			this.trackName = trackName;
		}else {
			this.trackName = "";
		}
		
		if(artistName != null) {
			this.artistName = artistName;
		}else {
			this.artistName = "";
		}
		
		if(albumInfo != null) {
			this.albumInfo = albumInfo;
		}else {
			this.albumInfo = "";
		}
		
		if(genre != null) {
			this.genre = genre;
		}else {
			this.genre = "";
		}
		
		this.musicFileExtract = musicFileExtract;
	}
	
	
	//Setters and getters of this class.
	
	public void setTrackName(String trackName){
		this.trackName = trackName;
	}
	
	public void setArtistName(String artistName){
		this.artistName = artistName;
	}
	
	public void setAlbumInfo(String albumInfo){
		this.albumInfo = albumInfo;
	}
	
	public void setGenre(String genre){
		this.genre = genre;
	}
	
	public void setMusicFileExtract(byte[] musicFileExtract){
		this.musicFileExtract = musicFileExtract;
	}
	
	public String getTrackName(){
		return this.trackName;
	}
	
	public String getArtistName(){
		return this.artistName;
	}
	
	public String getAlbumInfo(){
		return this.albumInfo;
	}
	
	public String getGenre(){
		return this.genre;
	}
	
	public byte[] getMusicFileExtract(){
		return this.musicFileExtract;
	}
}
