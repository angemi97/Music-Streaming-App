import java.io.Serializable;

/* Value objects are used to send the chunks of a certain song from Publisher -> Broker -> Consumer.
 * Serializable makes the objects of this class able to be converted into streams that are going to be 
 * sent/received from the ObjectInput/OutputStreams.
 */

public class Value implements Serializable{
	private static final long serialVersionUID = 1L;
	private MusicFile musicFile;
	
	//Class constructor.
	
	public Value(MusicFile musicFile) {
		this.musicFile = musicFile;
	}
	
	//Setters and getters of this class.
	
	public void setMusicFile(MusicFile musicFile){
		this.musicFile = musicFile;
	}
	
	public MusicFile getMusicFile(){
		return this.musicFile;
	}
	
	//Print returns the MusicFile's information (mainly used for debugging).
	
	public String print() {
		return musicFile.getArtistName() + ": " + musicFile.getTrackName() + " " + musicFile.getAlbumInfo() + " " + musicFile.getGenre() + " " + musicFile.getMusicFileExtract().length; 
	}
}