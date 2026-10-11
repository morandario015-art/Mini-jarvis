from vosk import Model, Recognizer, SetLogLevel
import json
SetLogLevel(-1)
m=Model("app/src/main/assets/model-en-us")
assert m.vosk_model_find_word("jarvis")>=0, "Jarvis missing from vocabulary"
r=Recognizer(m,16000,json.dumps(["jarvis","[unk]"]))
r.AcceptWaveform(bytes(32000))
assert json.loads(r.FinalResult()).get("text","")=="", "Silence must not trigger"
s=Model("app/src/main/assets/model-es")
assert s.vosk_model_find_word("abre")>=0
print("Jarvis vocabulary, Spanish command model and silence checks passed")
