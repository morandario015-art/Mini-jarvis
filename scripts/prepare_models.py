import pathlib, urllib.request, zipfile, io, shutil, uuid
root=pathlib.Path("app/src/main/assets")
for source,dest in [("vosk-model-small-en-us-0.15","model-en-us"),("vosk-model-small-es-0.42","model-es")]:
    data=urllib.request.urlopen("https://alphacephei.com/vosk/models/"+source+".zip",timeout=120).read()
    archive=zipfile.ZipFile(io.BytesIO(data))
    out=root/dest
    out.mkdir(parents=True,exist_ok=True)
    for entry in archive.infolist():
        relative=pathlib.PurePosixPath(entry.filename).relative_to(source)
        if entry.is_dir(): continue
        if ".." in relative.parts: raise ValueError("Unsafe model path")
        target=out.joinpath(*relative.parts);target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(archive.read(entry))
    (out/"uuid").write_text(source+"-jarvis-v03")
print("Offline English wake and Spanish command models bundled")
