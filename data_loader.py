import os
import pandas as pd
from app import db, app
from sqlalchemy import Column, String, Float, Enum
import uuid
from enum import Enum as PyEnum

class SDLCPhase(PyEnum):
    REQUIREMENTS = "REQUIREMENTS"
    DESIGN = "DESIGN"
    DEVELOPMENT = "DEVELOPMENT"
    TESTING = "TESTING"
    DEPLOYMENT = "DEPLOYMENT"
    MAINTENANCE = "MAINTENANCE"

class Project(db.Model):
    __tablename__ = "projects"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    projectName = Column(String, nullable=False)
    technology = Column(String, nullable=True)
    businessUnit = Column(String, nullable=True)
    sdlcPhase = Column(Enum(SDLCPhase), nullable=True)
    mtpMonth = Column(String, nullable=True)
    baselineEffortHours = Column(Float, nullable=True)
    aiAssistedEffortHours = Column(Float, nullable=True)
    aiUsageDescription = Column(String(2000), nullable=True)

    @property
    def hoursSaved(self):
        if self.baselineEffortHours is not None and self.aiAssistedEffortHours is not None:
            return self.baselineEffortHours - self.aiAssistedEffortHours
        return 0.0

def load_projects_from_excel(filepath: str = 'projects.xlsx'):
    """
    Load projects from an Excel file into the database.
    If the file is missing or invalid, logs error and returns safely.
    """
    if not os.path.isfile(filepath):
        app.logger.error(f"File not found: {filepath}")
        return

    try:
        df = pd.read_excel(filepath, engine='openpyxl')
    except Exception as e:
        app.logger.error(f"Error reading Excel file {filepath}: {e}")
        return

    required_columns = ['projectName', 'baselineEffortHours', 'aiAssistedEffortHours']
    for col in required_columns:
        if col not in df.columns:
            app.logger.error(f"Missing required column '{col}' in Excel file")
            return

    with app.app_context():
        db.session.query(Project).delete()

        for _, row in df.iterrows():
            baseline = row.get('baselineEffortHours')
            ai_assisted = row.get('aiAssistedEffortHours')
            if baseline is None or baseline <= 0:
                app.logger.warning(f"Skipping project with invalid baselineEffortHours: {baseline}")
                continue
            if ai_assisted is None:
                app.logger.warning("Skipping project with null aiAssistedEffortHours")
                continue
            if ai_assisted > baseline:
                app.logger.warning(f"Skipping project with aiAssistedEffortHours > baselineEffortHours: {ai_assisted} > {baseline}")
                continue

            sdlc_raw = row.get('sdlcPhase')
            sdlc_enum = None
            if isinstance(sdlc_raw, str):
                sdlc_clean = sdlc_raw.strip().upper().replace(" ", "_")
                if sdlc_clean in SDLCPhase.__members__:
                    sdlc_enum = SDLCPhase[sdlc_clean]

            project = Project(
                id=str(row['id']) if 'id' in row and isinstance(row['id'], str) and len(row['id']) == 36 else str(uuid.uuid4()),
                projectName=row['projectName'],
                technology=row.get('technology'),
                businessUnit=row.get('businessUnit'),
                sdlcPhase=sdlc_enum,
                mtpMonth=row.get('mtpMonth'),
                baselineEffortHours=baseline,
                aiAssistedEffortHours=ai_assisted,
                aiUsageDescription=row.get('aiUsageDescription')
            )
            db.session.add(project)

        db.session.commit()
        app.logger.info(f"Loaded {df.shape[0]} project records from {filepath}")
import pandas as pd
from sqlalchemy.orm import Session
from app import db
from enum import Enum
import logging

# Define SDLCPhase enum aligned with domain
class SDLCPhase(Enum):
    REQUIREMENTS = "REQUIREMENTS"
    DESIGN = "DESIGN"
    DEVELOPMENT = "DEVELOPMENT"
    TESTING = "TESTING"
    DEPLOYMENT = "DEPLOYMENT"
    MAINTENANCE = "MAINTENANCE"

# Define the ProjectEntity model for SQLAlchemy
class ProjectEntity(db.Model):
    __tablename__ = 'projects'

    id = db.Column(db.String(36), primary_key=True, default=lambda: str(uuid.uuid4()), nullable=False, unique=True)
    project_name = db.Column(db.String(255), nullable=False)
    technology = db.Column(db.String(255), nullable=True)
    business_unit = db.Column(db.String(255), nullable=True)
    sdlc_phase = db.Column(db.String(50), nullable=True)
    mtp_month = db.Column(db.String(50), nullable=True)
    baseline_effort_hours = db.Column(db.Float, nullable=True)
    ai_assisted_effort_hours = db.Column(db.Float, nullable=True)
    ai_usage_description = db.Column(db.String(2000), nullable=True)

    @property
    def hours_saved(self):
        try:
            if self.baseline_effort_hours is not None and self.ai_assisted_effort_hours is not None:
                return self.baseline_effort_hours - self.ai_assisted_effort_hours
            return 0.0
        except Exception:
            return 0.0

def load_projects_from_excel(filepath: str, session: Session):
    """
    Load projects from an Excel file into the database session.
    Expects columns:
      projectName, technology, businessUnit, sdlcPhase, mtpMonth,
      baselineEffortHours, aiAssistedEffortHours, aiUsageDescription
    Ignores rows missing projectName or baselineEffortHours.
    """
    try:
        df = pd.read_excel(filepath, engine='openpyxl')
    except Exception as e:
        logging.error(f"Failed to load Excel file {filepath}: {e}")
        return

    # Normalize column names to lowercase underscored internally
    df.columns = [str(c).strip() for c in df.columns]

    for idx, row in df.iterrows():
        try:
            project_name = row.get('projectName') or row.get('project_name')
            if not project_name or not isinstance(project_name, str) or project_name.strip() == '':
                continue

            baseline = row.get('baselineEffortHours') or row.get('baseline_effort_hours')
            if baseline is None or not isinstance(baseline, (int, float)):
                continue

            ai_assisted = row.get('aiAssistedEffortHours') or row.get('ai_assisted_effort_hours')
            if ai_assisted is None or not isinstance(ai_assisted, (int, float)):
                continue

            # Validate AI assisted hours do not exceed baseline
            if ai_assisted > baseline:
                logging.warning(f"Row {idx}: aiAssistedEffortHours > baselineEffortHours, skipping")
                continue

            sdlc_phase_raw = row.get('sdlcPhase') or row.get('sdlc_phase')
            sdlc_phase = None
            if isinstance(sdlc_phase_raw, str):
                sdlc_phase_val = sdlc_phase_raw.strip().upper().replace(' ', '_')
                if sdlc_phase_val in SDLCPhase.__members__:
                    sdlc_phase = sdlc_phase_val

            project = ProjectEntity(
                project_name=project_name.strip(),
                technology=row.get('technology'),
                business_unit=row.get('businessUnit') or row.get('business_unit'),
                sdlc_phase=sdlc_phase,
                mtp_month=row.get('mtpMonth') or row.get('mtp_month'),
                baseline_effort_hours=float(baseline),
                ai_assisted_effort_hours=float(ai_assisted),
                ai_usage_description=row.get('aiUsageDescription') or row.get('ai_usage_description')
            )
            session.merge(project)
        except Exception as ex:
            logging.error(f"Error processing row {idx}: {ex}")
    session.flush()
