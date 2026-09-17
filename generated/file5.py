# File: api.py
from flask import Blueprint, request, jsonify, current_app
from sqlalchemy import or_, asc, desc
from data_loader import ProjectEntity, SDLCPhase
from service import ProjectService
from app import db
import math

bp = Blueprint('api', __name__, url_prefix='/api/projects')


def project_to_dict(project: ProjectEntity):
    return {
        "id": project.id,
        "projectName": project.project_name,
        "technology": project.technology,
        "businessUnit": project.business_unit,
        "sdlcPhase": project.sdlc_phase.name if project.sdlc_phase else None,
        "mtpMonth": project.mtp_month,
        "baselineEffortHours": project.baseline_effort_hours,
        "aiAssistedEffortHours": project.ai_assisted_effort_hours,
        "aiUsageDescription": project.ai_usage_description,
        "hoursSaved": project.hours_saved,
    }


@bp.route('', methods=['GET'])
def search_projects():
    # Query params: searchTerm (default ""), page (default 0), size (default 10),
    # sort (field), direction (asc|desc)

    search_term = request.args.get('searchTerm', '', type=str).strip()
    page = request.args.get('page', 0, type=int)
    size = request.args.get('size', 10, type=int)
    sort = request.args.get('sort', 'projectName', type=str)
    direction = request.args.get('direction', 'asc', type=str).lower()

    # Validate size and page
    size = max(1, min(size, 100))
    page = max(0, page)

    # Map sort field from DTO to model column names
    sort_field_map = {
        "projectName": ProjectEntity.project_name,
        "technology": ProjectEntity.technology,
        "businessUnit": ProjectEntity.business_unit,
        "sdlcPhase": ProjectEntity.sdlc_phase,
        "mtpMonth": ProjectEntity.mtp_month,
        "baselineEffortHours": ProjectEntity.baseline_effort_hours,
        "aiAssistedEffortHours": ProjectEntity.ai_assisted_effort_hours,
        "hoursSaved": None,  # computed, sort by baseline - ai_assisted
    }

    sort_col = sort_field_map.get(sort, ProjectEntity.project_name)

    # Build base query
    query = ProjectEntity.query

    if search_term:
        search_term_like = f"%{search_term}%"
        query = query.filter(
            or_(
                ProjectEntity.project_name.ilike(search_term_like),
                ProjectEntity.technology.ilike(search_term_like),
                ProjectEntity.business_unit.ilike(search_term_like),
            )
        )

    # Apply sorting
    if sort_col is None and sort == "hoursSaved":
        # Sort by computed column: baseline_effort_hours - ai_assisted_effort_hours
        comp_expr = ProjectEntity.baseline_effort_hours - ProjectEntity.ai_assisted_effort_hours
        if direction == 'desc':
            query = query.order_by(desc(comp_expr))
        else:
            query = query.order_by(asc(comp_expr))
    else:
        if direction == 'desc':
            query = query.order_by(desc(sort_col))
        else:
            query = query.order_by(asc(sort_col))

    total = query.count()
    projects = query.offset(page * size).limit(size).all()

    content = [project_to_dict(p) for p in projects]

    response = {
        "content": content,
        "totalElements": total,
        "totalPages": math.ceil(total / size) if size else 1,
        "number": page,
        "size": size,
    }
    return jsonify(response)


@bp.route('/<string:project_id>', methods=['GET'])
def get_project(project_id):
    project = ProjectEntity.query.get(project_id)
    if not project:
        return jsonify({"error": "Project not found"}), 404
    return jsonify(project_to_dict(project))


@bp.route('', methods=['POST'])
def create_project():
    data = request.get_json()
    if not data:
        return jsonify({"error": "Missing JSON body"}), 400

    # Validate required fields
    try:
        project_name = data['projectName']
        baseline_effort = float(data['baselineEffortHours'])
        ai_assisted_effort = float(data['aiAssistedEffortHours'])
    except (KeyError, ValueError, TypeError):
        return jsonify({"error": "Invalid or missing required fields"}), 400

    if not project_name or baseline_effort <= 0 or ai_assisted_effort < 0 or ai_assisted_effort > baseline_effort:
        return jsonify({"error": "Business rules violated for effort hours or projectName"}), 400

    # Create new project
    from data_loader import SDLCPhase
    from uuid import uuid4

    sdlc_phase_str = data.get('sdlcPhase')
    sdlc_phase = None
    if sdlc_phase_str:
        try:
            sdlc_phase = SDLCPhase[sdlc_phase_str.upper().replace(" ", "_")]
        except KeyError:
            sdlc_phase = None

    project = ProjectEntity(
        id=str(uuid4()),
        project_name=project_name,
        technology=data.get('technology'),
        business_unit=data.get('businessUnit'),
        sdlc_phase=sdlc_phase,
        mtp_month=data.get('mtpMonth'),
        baseline_effort_hours=baseline_effort,
        ai_assisted_effort_hours=ai_assisted_effort,
        ai_usage_description=data.get('aiUsageDescription'),
    )
    db.session.add(project)
    db.session.commit()

    return jsonify(project_to_dict(project)), 201


@bp.route('/<string:project_id>', methods=['PUT'])
def update_project(project_id):
    project = ProjectEntity.query.get(project_id)
    if not project:
        return jsonify({"error": "Project not found"}), 404

    data = request.get_json()
    if not data:
        return jsonify({"error": "Missing JSON body"}), 400

    # Validate required fields
    try:
        project_name = data['projectName']
        baseline_effort = float(data['baselineEffortHours'])
        ai_assisted_effort = float(data['aiAssistedEffortHours'])
    except (KeyError, ValueError, TypeError):
        return jsonify({"error": "Invalid or missing required fields"}), 400

    if not project_name or baseline_effort <= 0 or ai_assisted_effort < 0 or ai_assisted_effort > baseline_effort:
        return jsonify({"error": "Business rules violated for effort hours or projectName"}), 400

    from data_loader import SDLCPhase

    sdlc_phase_str = data.get('sdlcPhase')
    sdlc_phase = None
    if sdlc_phase_str:
        try:
            sdlc_phase = SDLCPhase[sdlc_phase_str.upper().replace(" ", "_")]
        except KeyError:
            sdlc_phase = None

    project.project_name = project_name
    project.technology = data.get('technology')
    project.business_unit = data.get('businessUnit')
    project.sdlc_phase = sdlc_phase
    project.mtp_month = data.get('mtpMonth')
    project.baseline_effort_hours = baseline_effort
    project.ai_assisted_effort_hours = ai_assisted_effort
    project.ai_usage_description = data.get('aiUsageDescription')

    db.session.commit()

    return jsonify(project_to_dict(project))


@bp.route('/<string:project_id>', methods=['DELETE'])
def delete_project(project_id):
    project = ProjectEntity.query.get(project_id)
    if not project:
        return jsonify({"error": "Project not found"}), 404
    db.session.delete(project)
    db.session.commit()
    return '', 204


@bp.route('/analytics/kpis', methods=['GET'])
def get_kpis():
    kpis = ProjectService.get_aggregate_kpis()
    return jsonify(kpis)


@bp.route('/analytics/effort-by-sdlc-phase', methods=['GET'])
def effort_by_sdlc_phase():
    data = ProjectService.get_effort_by_sdlc_phase()
    return jsonify(data)


@bp.route('/analytics/effort-by-technology', methods=['GET'])
def effort_by_technology():
    data = ProjectService.get_effort_by_technology()
    return jsonify(data)


@bp.route('/analytics/effort-by-business-unit', methods=['GET'])
def effort_by_business_unit():
    data = ProjectService.get_effort_by_business_unit()
    return jsonify(data)