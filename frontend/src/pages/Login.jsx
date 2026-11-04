import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import AuthLayout from '../components/AuthLayout.jsx'
import FormField from '../components/FormField.jsx'
import Alert from '../components/Alert.jsx'
import Spinner from '../components/Spinner.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form.email.trim(), form.password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to manage your tasks"
      footer={<>New here? <Link to="/register" className="font-medium text-indigo-600 hover:underline">Create an account</Link></>}
    >
      <Alert>{error}</Alert>
      <form onSubmit={onSubmit} className="space-y-4">
        <FormField label="Email" name="email" type="email" autoComplete="email" required
          value={form.email} onChange={onChange} />
        <FormField label="Password" name="password" type="password" autoComplete="current-password" required
          value={form.password} onChange={onChange} />
        <button type="submit" disabled={loading}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-60">
          {loading && <Spinner />} Sign in
        </button>
      </form>
    </AuthLayout>
  )
}
