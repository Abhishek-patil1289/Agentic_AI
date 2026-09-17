# File: service.py
from collections import defaultdict, OrderedDict
from decimal import Decimal, ROUND_HALF_UP
from sqlalchemy import func, case
from app import db
from data_loader import ProjectEntity, SDLCPhase


class ProjectRepository:
    """Repository for ProjectEntity queries."""

    @staticmethod
    def get_all_projects():
        return ProjectEntity.query.all()

    @staticmethod
    def get_effort_sums():
        """Return sums of baseline, ai_assisted, hours_saved over all projects."""
        baseline_sum = db.session.query(func.sum(ProjectEntity.baseline_effort_hours)).scalar() or 0.0
        ai_assisted_sum = db.session.query(func.sum(ProjectEntity.ai_assisted_effort_hours)).scalar() or 0.0
        # hours_saved is baseline - ai_assisted, but sum over rows:
        # sum(baseline - ai_assisted) = sum(baseline) - sum(ai_assisted)
        hours_saved_sum = baseline_sum - ai_assisted_sum
        return baseline_sum, ai_assisted_sum, hours_saved_sum

    @staticmethod
    def group_effort_by_column(column):
        """
        Aggregate baseline, ai_assisted, hours_saved grouped by a given column
        (e.g. ProjectEntity.sdlc_phase, ProjectEntity.technology, ProjectEntity.business_unit).
        Null values normalized to string "UNKNOWN".
        Returns OrderedDict keyed by str(column) with dict of sums.
        """
        # Build label for grouping: convert None to "UNKNOWN"
        label = func.coalesce(column, "UNKNOWN").label('group_key')

        baseline_sum = func.sum(ProjectEntity.baseline_effort_hours)
        ai_assisted_sum = func.sum(ProjectEntity.ai_assisted_effort_hours)

        # Group query
        query = (
            db.session.query(
                label,
                baseline_sum,
                ai_assisted_sum,
                (baseline_sum - ai_assisted_sum).label('hours_saved')
            )
            .group_by(label)
            .order_by(label)
        )
        results = query.all()

        ordered = OrderedDict()
        for group_key, baseline, ai_assisted, hours_saved in results:
            ordered[group_key] = {
                'baselineEffortHours': float(baseline or 0.0),
                'aiAssistedEffortHours': float(ai_assisted or 0.0),
                'hoursSaved': float(hours_saved or 0.0)
            }
        return ordered


class ProjectService:
    """Service layer providing KPI and aggregation computations."""

    @staticmethod
    def get_aggregate_kpis():
        baseline_sum, ai_assisted_sum, hours_saved_sum = ProjectRepository.get_effort_sums()

        if baseline_sum > 0:
            avg_savings_percent = (Decimal(hours_saved_sum) / Decimal(baseline_sum) * Decimal(100)).quantize(
                Decimal('0.01'), rounding=ROUND_HALF_UP)
        else:
            avg_savings_percent = Decimal('0.00')

        return OrderedDict([
            ('totalBaselineEffortHours', float(baseline_sum)),
            ('totalAiAssistedEffortHours', float(ai_assisted_sum)),
            ('totalHoursSaved', float(hours_saved_sum)),
            ('averageSavingsPercent', float(avg_savings_percent)),
        ])

    @staticmethod
    def get_effort_by_sdlc_phase():
        return ProjectRepository.group_effort_by_column(ProjectEntity.sdlc_phase)

    @staticmethod
    def get_effort_by_technology():
        return ProjectRepository.group_effort_by_column(ProjectEntity.technology)

    @staticmethod
    def get_effort_by_business_unit():
        return ProjectRepository.group_effort_by_column(ProjectEntity.business_unit)