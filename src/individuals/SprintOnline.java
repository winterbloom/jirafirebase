package individuals;

/*********************************************************
 * Description:
 *
 * Contains some helper functions for parsing json
 *
 * User: Hazel
 * Date: 8/9/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class SprintOnline {
    private String id;
    private String name;
    private String state;

    public SprintOnline() {

    }

    public SprintOnline(String id, String name, String state) {
        this.id = id;
        this.name = name;
        this.state = state;
    }

    public String getID() { return id; }

    public String getName() { return name; }

    public String getState() { return state; }

    public void setID(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Sprint: {id: " + id + " name: '" + name + "' state: " + state + "}";
    }

    @Override
    public boolean equals(Object other) {
        SprintOnline o = (SprintOnline) other;
        return (this.getID().equals(o.getID())) &&
                (this.getName().equals(o.getName())) &&
                (this.getState().equals(o.getState()));
    }
}
