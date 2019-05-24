package net.refractions.chyf.hygraph;

public class Coverage {
	
	private int id;
	private String name;
	private double pourcent;
	
	public Coverage (int id,String name, double pourcent)
	{
		this.id = id;
		this.name = name;
		this.pourcent = pourcent;
	}
	
	public Coverage (int id, double pourcent)
	{
		this.id = id;
		this.pourcent = pourcent;
		
		setNameWithId(this.id);
	}
	
	public int getId() {
		return this.id;
	}
	public String getName() {
		return this.name;
	}
	
	public double getPourcent() {
		return this.pourcent;
	}

	//le nom de la variable se change en même temps que le id
	public void setId(int id)
	{
		this.id = id;
		setNameWithId(this.id);
	}
	
	public void setName(String name)
	{
		this.name = name;
		
	}
	
	public void setPourcent(double pourcent)
	{
		this.pourcent = pourcent;
	}
	
	private void setNameWithId(int id)
	{
		switch (id) 
		{
			case 1:
				this.name = "class1";
				break;
			case 2:
				this.name = "class2";
				break;
			case 3:
				this.name = "class3";
				break;
			case 4:
				this.name = "class4";
				break;
			case 5:
				this.name = "class5";
				break;
			case 6:
				this.name = "class6";
				break;
			case 7:
				this.name = "class7";
				break;
			case 8:
				this.name = "class8";
				break;
			case 9:
				this.name = "class9";
				break;
			case 10:
				this.name = "class10";
				break;
			case 11:
				this.name = "class11";
				break;
			case 12:
				this.name = "class12";
				break;
			case 13:
				this.name = "class13";
				break;
			case 14:
				this.name = "class14";
				break;
			case 15:
				this.name = "class15";
				break;
			case 16:
				this.name = "class16";
				break;
			case 17:
				this.name = "class17";
				break;
			case 18:
				this.name = "class18";
				break;
			case 19:
				this.name = "class19";
				break;
		}
	}
}
