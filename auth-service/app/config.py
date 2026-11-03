from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Local defaults; overridden by environment variables in Docker/AWS
    database_url: str = "postgresql+psycopg://auth_user:auth_pass@localhost:5432/auth_db"
    jwt_secret: str = "local-dev-secret-change-me-must-be-32-plus-bytes"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60
    cors_origins: str = "http://localhost:5173"

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()