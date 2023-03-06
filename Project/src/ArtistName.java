import java.io.Serializable;

/*
 * ArtistName objects are used to store an artist's name.
 * Mainly used in the hashing process to find which artist is assigned 
 * to a certain Broker.
 */

public class ArtistName implements Serializable{
	private static final long serialVersionUID = 1L;
	private String artistName;
	
	//Class constructor.
	
	public ArtistName(String artistName) {
		this.artistName = artistName;
	}
	
	//Setters and getters of this class.
	
	public void setArtistName(String artistName){
		this.artistName = artistName;
	}
	
	public String getArtistName(){
		return this.artistName;
	}
}
