package io.thedal;

public class Log {
  private long id;
  private String log;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getLog() {
    return log;
  }

  public void setLog(String log) {
    this.log = log;
  }

  @Override
  public String toString() {
    return log;
  }
}
