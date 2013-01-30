import
  java.io.File

val file = new File("/home/jason/Desktop/NetLogoLogs/pruned/")
file.listFiles().toSeq.par foreach (file => io.Source.fromFile(file).getLines() foreach LogParser.apply)
