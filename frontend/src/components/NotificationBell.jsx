import { useCallback, useEffect, useRef, useState } from 'react'
import { notificationApi } from '../api.js'
import Spinner from './Spinner.jsx'

const POLL_MS = 10_000

// "just now", "5 min ago", "3 h ago"; older than a day shows the date and time
function formatTime(iso) {
  const date = new Date(iso)
  const minutes = Math.floor((Date.now() - date.getTime()) / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  if (minutes < 24 * 60) return `${Math.floor(minutes / 60)} h ago`
  return date.toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function NotificationBell() {
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const ref = useRef(null)

  const refreshCount = useCallback(async () => {
    try {
      const { count } = await notificationApi.unreadCount()
      setUnread(count)
    } catch {
      // Ignore; the next poll will retry (a 401 already logs the user out)
    }
  }, [])

  // Poll the unread count every 10 seconds
  useEffect(() => {
    refreshCount()
    const id = setInterval(refreshCount, POLL_MS)
    return () => clearInterval(id)
  }, [refreshCount])

  // Close on outside click or Escape
  useEffect(() => {
    if (!open) return
    const onClick = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false)
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onClick)
    window.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      window.removeEventListener('keydown', onKey)
    }
  }, [open])

  // Opening loads the list, then marks everything read. Items keep their
  // "unread" highlight from the fetch so the user can see what's new.
  const toggle = async () => {
    if (open) {
      setOpen(false)
      return
    }
    setOpen(true)
    setLoading(true)
    setError('')
    try {
      const list = await notificationApi.list()
      setItems(list)
      if (list.some((n) => !n.read)) {
        await notificationApi.markAllRead()
      }
      setUnread(0)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative" ref={ref}>
      <button onClick={toggle} aria-label={unread ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
        className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900">
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
        </svg>
        {unread > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] leading-none font-semibold text-white ring-2 ring-white">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-40 mt-2 w-80 max-w-[calc(100vw-2rem)] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
          <div className="border-b border-slate-100 px-4 py-2.5">
            <h2 className="text-sm font-semibold text-slate-900">Notifications</h2>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="flex justify-center py-8 text-indigo-600"><Spinner /></div>
            ) : error ? (
              <p className="px-4 py-6 text-center text-sm text-red-600">{error}</p>
            ) : items.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-slate-500">No notifications yet</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {items.map((n) => (
                  <li key={n.id} className={`flex gap-3 px-4 py-3 ${n.read ? '' : 'bg-indigo-50/70'}`}>
                    <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${n.read ? 'bg-transparent' : 'bg-indigo-500'}`} aria-hidden="true" />
                    <div className="min-w-0">
                      <p className={`text-sm break-words ${n.read ? 'text-slate-600' : 'font-medium text-slate-900'}`}>{n.message}</p>
                      <time dateTime={n.createdAt} title={new Date(n.createdAt).toLocaleString()}
                        className="mt-0.5 block text-xs text-slate-400">
                        {formatTime(n.createdAt)}
                      </time>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
