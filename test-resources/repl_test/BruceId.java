package net.lfn3.sprue.BruceId;

import java.lang.Object;
import java.lang.String;
import javax.annotation.processing.Generated;

@Generated("net.lfn3.sprue.java_output_from_normalized.clj")
public class BruceId {
  private final long id;

  public BruceId(long id) {
    super(id);
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public boolean equals(public Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (this == o) return true;
    BruceId that = (BruceId) o;
    return super.equals(that) &&
    	Objects.equals(this.id, that.id);
  }

  public int hashcode() {
    Objects.hash(id);
  }

  public String toString() {
    "BruceId{"+ ", " + 
     Object.toString(id)"}" + super.toString();
  }
}
