import { useState } from 'react'
import { Icon } from './Icons'

/** Privacy policy and terms of use: what the launcher really does with data (kept in sync with the code) */
const PRIVACY: [string, string[]][] = [
  ['Что хранится на вашем компьютере', [
    'Аккаунты: офлайн-ники и данные входа Microsoft. Данные входа хранятся только на этом компьютере в зашифрованном виде и никуда не передаются, кроме серверов Microsoft и Mojang при входе.',
    'Настройки лаунчера и визуалов, скины, плащи, метки, бинды, миры и моды — в папке лаунчера и папке игры.']],
  ['Что отправляется на сервер Asteria', [
    'Ник и служебный ключ, который подтверждает, что ник ваш.',
    'Скин, плащ, питомец, косметика и свои 3D-модели — чтобы их видели другие игроки AsteriaLauncher.',
    'Статус в игре (версия, сервер, адрес открытого мира) — чтобы друзья видели, где вы играете.',
    'Список друзей, заявки, код дружбы; коды «Поделиться» и коды сохранения — только когда вы сами их создаёте.',
    'Пароли и данные входа Microsoft на сервер Asteria не отправляются никогда.']],
  ['Другие сервисы', [
    'Modrinth — поиск и скачивание модов, ресурспаков и шейдеров.',
    'Mojang и Microsoft — загрузка игры и вход в лицензионный аккаунт.',
    'Перевод интерфейса (Google Translate, Lingva, MyMemory) — уходят только тексты интерфейса лаунчера, без личных данных.',
    'Discord — статус «Играет в Minecraft», если он включён.',
    '«Сейчас играет» читается с вашего компьютера и никуда не отправляется.']],
  ['Сколько хранится и как удалить', [
    'Данные на сервере хранятся, пока вы пользуетесь лаунчером. Коды сохранения и «Поделиться» могут удаляться со временем.',
    'Всё локальное можно удалить в Настройках → Удаление. Чтобы удалить данные с сервера, напишите в Telegram @filonis_music.']],
  ['Чего мы не делаем', ['Не продаём и не передаём ваши данные третьим лицам, не показываем рекламу и не собираем телеметрию о вашем компьютере.']],
]
const TERMS: [string, string[]][] = [
  ['Общее', ['AsteriaLauncher — неофициальный лаунчер Minecraft. Он не связан с Mojang Studios и Microsoft. Minecraft — товарный знак Mojang.',
    'Пользуясь лаунчером, вы соглашаетесь с этими условиями. Если не согласны — не используйте лаунчер.']],
  ['Ваш контент', ['Скины, плащи, GIF, 3D-модели и картинки, которые вы загружаете, видят другие игроки. Загружайте только то, на что у вас есть права.',
    'Запрещено загружать оскорбительное, незаконное и чужое без разрешения. Такой контент может быть удалён, а доступ к функциям ограничен.']],
  ['Честная игра', ['Визуалы Asteria меняют только то, что видите вы, и не дают преимущества. Не используйте лаунчер вместе с читами.',
    'На каждом сервере действуют свои правила — их нужно соблюдать. Наказания серверов от лаунчера не зависят.']],
  ['Подписка и аккаунт', ['PREMIUM открывает дополнительные визуалы на срок подписки. Ник, привязанный к лаунчеру, нельзя передавать другим людям.',
    'Код сохранения «Для себя» содержит ваши ники — никому его не отдавайте.']],
  ['Ответственность', ['Лаунчер предоставляется «как есть». Мы стараемся, чтобы всё работало, но не отвечаем за потерю миров и файлов — делайте копии важного.',
    'Условия могут меняться; новая версия действует с выхода обновления лаунчера.']],
  ['Связь', ['Вопросы, удаление данных, сотрудничество: Telegram @filonis_music.']],
]

export function LegalCard() {
  const [open, setOpen] = useState<'' | 'privacy' | 'terms'>('')
  const doc = open === 'privacy' ? PRIVACY : TERMS
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div className="font-bold flex items-center gap-2"><Icon n="shield" s={18} />Документы</div>
    <div className="flex flex-col gap-2">
      <button className="mcard !p-3 flex items-center gap-3 text-left" onClick={() => setOpen('privacy')}><Icon n="lock" s={16} /><div className="flex-1"><b className="text-sm block">Политика конфиденциальности</b><span className="muted text-[11px]">Какие данные есть у лаунчера и куда они уходят</span></div><Icon n="right" s={14} /></button>
      <button className="mcard !p-3 flex items-center gap-3 text-left" onClick={() => setOpen('terms')}><Icon n="rules" s={16} /><div className="flex-1"><b className="text-sm block">Условия использования</b><span className="muted text-[11px]">Правила для контента, честная игра, ответственность</span></div><Icon n="right" s={14} /></button></div>
    {open && <div className="ask-bg" onClick={() => setOpen('')}><div className="ask !w-[620px] max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
      <div className="flex items-center justify-between mb-3"><b className="text-xl">{open === 'privacy' ? 'Политика конфиденциальности' : 'Условия использования'}</b><button className="iconbtn" onClick={() => setOpen('')}><Icon n="close" s={15} /></button></div>
      <div className="overflow-auto pr-2 flex flex-col gap-4">{doc.map(([h, ps]) => <div key={h}><b className="block mb-1">{h}</b>{ps.map((p) => <p key={p} className="muted text-sm mb-1.5 leading-relaxed">{p}</p>)}</div>)}
        <p className="muted text-[11px]">Действует с версии 1.6.0 AsteriaLauncher.</p></div></div></div>}
  </div>
}
