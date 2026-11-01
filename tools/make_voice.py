"""The spoken phrases of Asteria Visuals in one voice: Silero TTS v5_cis_base_nostress (MIT licence), speaker ru_ekaterina, natural pitch.
  1. download https://models.silero.ai/models/tts/ru/v5_cis_base_nostress.pt (~92 MB, needs torch + soundfile)
  2. python tools/make_voice.py <model.pt> mods/common-visuals/resources/assets/asteria_visuals/sounds <loudness reference .ogg>
Stress: "+" before the stressed vowel (this model does not guess it for Russian)."""
import sys, torch, numpy as np, soundfile as sf
model = torch.package.PackageImporter(sys.argv[1]).load_pickle("tts_models", "model"); out, ref = sys.argv[2], sys.argv[3]
# unstressed vowels written as they sound ("здар+овья", "т+отэм"): this base model does not reduce them by itself, and a full "о" sounded like a wrong stress
LINES = {'wv_enable': 'Ф+ункция включен+а!', 'wv_disable': 'Ф+ункция в+ыключена.', 'wv_totem1': 'Т+отэм исп+ользаван!', 'wv_totem2': 'Т+отэм сп+ас тиб+я!',
  'wv_tother1': 'Прат+ивник патир+ял т+отэм!', 'wv_tother2': 'Вр+аг аст+ался без т+отэма!', 'wv_tother3': 'У враг+а сгар+ел т+отэм!', 'wv_tother4': 'Мин+ус т+отэм у прат+ивника!',
  'wv_hp': 'М+ала здар+овья!', 'wv_armor': 'Бр+оня лам+ается!', 'wv_food': 'Пар+а пак+ушать!', 'wv_effect': 'Эфф+ект зак+анчивается.',
  'wv_enemy_armor': 'У прат+ивника лам+ается бр+оня!', 'wv_shield': 'Щ+ит сл+оман!', 'wv_friend': 'Тиб+е п+ишет др+уг!'}
r, sr = sf.read(ref); r = r.mean(1) if r.ndim > 1 else r; rms_ref = np.sqrt((r[np.abs(r) > 0.01] ** 2).mean())
for name, text in LINES.items():
    a = model.apply_tts(text=text, speaker='ru_ekaterina', sample_rate=48000).numpy().astype(np.float64) # the natural pitch: raising it made the voice sound robotic
    nz = np.where(np.abs(a) > 0.01)[0]; a = a[max(0, nz[0] - 960): nz[-1] + 4800]          # 20 ms before, 100 ms after the voice
    rms = np.sqrt((a[np.abs(a) > 0.01] ** 2).mean()); a *= rms_ref / rms                   # as loud as the old voice
    a = np.clip(a, -0.97, 0.97); f = np.linspace(0, 1, 480); a[:480] *= f; a[-2400:] *= np.linspace(1, 0, 2400)
    sf.write(f'{out}/{name}.ogg', a, 48000, format='OGG', subtype='VORBIS'); print(name, round(len(a) / 48000, 2), 's')
