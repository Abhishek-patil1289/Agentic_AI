# File: data_loader.py
import os
import uuid
import logging
import pandas as pd
from enum import Enum
from flask import current_app
from app import db
from sqlalchemy.orm import validates

# SDLCPhase enum
class SDLCPhase(Enum):
    REQUIREMENTS = "REQUIREMENTS"
    DESIGN = "DESIGN"
    DEVELOPMENT = "DEVELOPMENT"
    TESTING = "TESTING"
    DEPLOYMENT = "DEPLOYMENT"
    MAINTENANCE = "MAINTENANCE"

# ProjectEntity model
class ProjectEntity(db.Model):
    __tablename__ = 'projects'

    id = db.Column(db.String(36), primary_key=True, default=lambda: str(uuid.uuid4()), nullable=False)
    project_name = db.Column(db.String(255), nullable=False)
    technology = db.Column(db.String(100), nullable=True)
    business_unit = db.Column(db.String(100), nullable=True)
    sdlc_phase = db.Column(db.Enum(SDLCPhase), nullable=True)
    mtp_month = db.Column(db.String(20), nullable=True)
    baseline_effort_hours = db.Column(db.Float, nullable=True)
    ai_assisted_effort_hours = db.Column(db.Float, nullable=True)
    ai_usage_description = db.Column(db.String(2000), nullable=True)

    @property
    def hours_saved(self):
        if self.baseline_effort_hours is not None and self.ai_assisted_effort_hours is not None:
            return self.baseline_effort_hours - self.ai_assisted_effort_hours
        return 0.0

def load_projects_from_csv(filepath=None):
    filepath = filepath or os.getenv('PROJECTS_CSV_PATH', 'projects.csv')
    if not os.path.isfile(filepath):
        current_app.logger.error(f"Data file not found: {filepath}")
        return

    try:
        df = pd.read_csv(filepath)
    except Exception as e:
        current_app.logger.error(f"Failed to load CSV file {filepath}: {e}")
        return

    # Validate columns presence
    expected_cols = {
        'id', 'projectName', 'technology', 'businessUnit', 'sdlcPhase',
        'mtpMonth', 'baselineEffortHours', 'aiAssistedEffortHours', 'aiUsageDescription'
    }
    missing_cols = expected_cols - set(df.columns)
    if missing_cols:
        current_app.logger.error(f"CSV missing columns: {missing_cols}")
        return

    for _, row in df.iterrows():
        # Validate or generate UUID
        try:
            project_id = str(row['id'])
            if len(project_id) != 36:
                raise ValueError
        except Exception:
            project_id = str(uuid.uuid4())

        sdlc_value = row.get('sdlcPhase')
        sdlc_enum = None
        if isinstance(sdlc_value, str):
            try:
                sdlc_enum = SDLCPhase[sdlc_value.upper()]
            except KeyError:
                sdlc_enum = None

        # Parse effort hours safely
        try:
            baseline = float(row['baselineEffortHours'])
        except Exception:
            baseline = None

        try:
            ai_assisted = float(row['aiAssistedEffortHours'])
        except Exception:
            ai_assisted = None

        project = ProjectEntity(
            id=project_id,
            project_name=row['projectName'],
            technology=row.get('technology'),
            business_unit=row.get('businessUnit'),
            sdlc_phase=sdlc_enum,
            mtp_month=row.get('mtpMonth'),
            baseline_effort_hours=baseline,
            ai_assisted_effort_hours=ai_assisted,
            ai_usage_description=row.get('aiUsageDescription')
        )
        db.session.merge(project)  # merge to update or insert

    try:
        db.session.commit()
        current_app.logger.info(f"Loaded {len(df)} project records from {filepath}")
    except Exception as e:
        current_app.logger.error(f"Failed to commit projects to DB: {e}")
        db.session.rollback()