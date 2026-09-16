import os
from datetime import datetime, timedelta
from functools import wraps

from flask import (
    Flask,
    jsonify,
    request,
    abort,
    render_template,
    redirect,
    url_for,
    session,
)
from flask_sqlalchemy import SQLAlchemy
from passlib.hash import bcrypt
import jwt
from dotenv import load_dotenv

# Load environment variables from .env file if present
load_dotenv()

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv(
    "DATABASE_URL", "sqlite:///app.db"
)
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SECRET_KEY"] = os.getenv("SECRET_KEY", "change_this_secret_key")
app.config["JWT_SECRET_KEY"] = os.getenv("JWT_SECRET_KEY", "change_this_jwt_secret")
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_ACCESS_TOKEN_EXPIRES"] = 30  # minutes
app.config["UPLOAD_FOLDER"] = os.getenv("UPLOAD_FOLDER", "uploads")
app.config["MAX_CONTENT_LENGTH"] = 16 * 1024 * 1024  # 16 MB max upload size

db = SQLAlchemy(app)


# Models
class User(db.Model):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False, index=True)
    email = db.Column(db.String(120), unique=True, nullable=False)
    full_name = db.Column(db.String(120), nullable=True)
    hashed_password = db.Column(db.String(255), nullable=False)
    disabled = db.Column(db.Boolean, default=False, nullable=False)

    def verify_password(self, password: str) -> bool:
        return bcrypt.verify(password, self.hashed_password)

    def to_dict(self):
        return {
            "id": self.id,
            "username": self.username,
            "email": self.email,
            "full_name": self.full_name,
            "disabled": self.disabled,
        }


# JWT related functions
def create_access_token(identity: str, expires_delta: timedelta = None):
    if expires_delta is None:
        expires_delta = timedelta(minutes=app.config["JWT_ACCESS_TOKEN_EXPIRES"])
    expire = datetime.utcnow() + expires_delta
    payload = {"sub": identity, "exp": expire, "iat": datetime.utcnow()}
    token = jwt.encode(payload, app.config["JWT_SECRET_KEY"], algorithm=app.config["JWT_ALGORITHM"])
    if isinstance(token, bytes):
        token = token.decode("utf-8")
    return token


def decode_access_token(token: str):
    try:
        payload = jwt.decode(
            token,
            app.config["JWT_SECRET_KEY"],
            algorithms=[app.config["JWT_ALGORITHM"]],
        )
        return payload["sub"]
    except jwt.ExpiredSignatureError:
        abort(401, description="Token expired")
    except jwt.InvalidTokenError:
        abort(401, description="Invalid token")


def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get("Authorization", None)
        if not auth_header or not auth_header.startswith("Bearer "):
            abort(401, description="Authorization header missing or invalid")
        token = auth_header.split(" ")[1]
        username = decode_access_token(token)
        user = User.query.filter_by(username=username).first()
        if not user or user.disabled:
            abort(401, description="Invalid or inactive user")
        return f(user, *args, **kwargs)

    return decorated


# Routes
@app.route("/")
def index():
    return render_template("index.html")


@app.route("/token", methods=["POST"])
def login():
    if not request.is_json:
        abort(400, description="Missing JSON in request")
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    if not username or not password:
        abort(400, description="Username and password required")
    user = User.query.filter_by(username=username).first()
    if not user or not user.verify_password(password):
        abort(401, description="Incorrect username or password")
    if user.disabled:
        abort(403, description="User account disabled")
    access_token = create_access_token(user.username)
    return jsonify(access_token=access_token, token_type="bearer")


@app.route("/users/me")
@token_required
def get_current_user(current_user):
    return jsonify(current_user.to_dict())


# Setup upload folder
if not os.path.exists(app.config["UPLOAD_FOLDER"]):
    os.makedirs(app.config["UPLOAD_FOLDER"])

if __name__ == "__main__":
    with app.app_context():
        db.create_all()
    app.run(debug=True, host="0.0.0.0", port=8000)
from flask import Flask
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)
app.config.from_mapping(
    SECRET_KEY='dev',
    SQLALCHEMY_DATABASE_URI='sqlite:///productivity_tracker.db',
    SQLALCHEMY_TRACK_MODIFICATIONS=False,
)

db = SQLAlchemy(app)

if __name__ == '__main__':
    app.run(debug=True, port=5000)
import os
from flask import Flask, render_template, request, redirect, url_for, flash, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_wtf.csrf import CSRFProtect
from sqlalchemy.dialects.postgresql import UUID
import uuid

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'devsecretkey')
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///productivitytracker.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)
csrf = CSRFProtect(app)

from data_loader import ProjectEntity, SDLCPhase, load_projects_from_excel

@app.before_first_request
def initialize_database():
    db.create_all()
    # Load initial data from default spreadsheet file if exists
    default_path = os.environ.get('PROJECTS_XLSX_PATH', 'projects.xlsx')
    if os.path.isfile(default_path):
        load_projects_from_excel(default_path, db.session)
        db.session.commit()

@app.route('/')
def index():
    return redirect(url_for('projects'))

@app.route('/projects')
def projects():
    # Placeholder for UI - will serve a template later
    return render_template('projects.html')

# Additional routes and API endpoints will be added later

if __name__ == '__main__':
    app.run(port=8080, debug=True)
