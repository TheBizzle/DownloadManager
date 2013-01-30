import
  java.io.File

val file = new File("/home/jason/Desktop/NetLogoLogs/unpruned/")
file.listFiles().toSeq.par foreach LogPruner.apply

