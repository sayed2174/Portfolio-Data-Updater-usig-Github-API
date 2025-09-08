# github_api.py
import json
import base64
import requests

GITHUB_API = 'https://api.github.com'

def _headers(pat):
    return {
        'Authorization': f'token {pat}',
        'Accept': 'application/vnd.github.v3+json'
    }

def list_json_files(owner, repo, pat, path='', ref=None):
    """
    List .json files in a path. ref = branch or commit sha (optional)
    Returns JSON string list of {name, path, sha, download_url}
    """
    url = f"{GITHUB_API}/repos/{owner}/{repo}/contents/{path}"
    params = {}
    if ref:
        params['ref'] = ref
    r = requests.get(url, headers=_headers(pat), params=params)
    r.raise_for_status()
    items = r.json()
    out = []
    for it in items:
        if it.get('type') == 'file' and it.get('name','').lower().endswith('.json'):
            out.append({
                'name': it['name'],
                'path': it['path'],
                'sha': it['sha'],
                'download_url': it.get('download_url')
            })
    return json.dumps(out)

def get_file_content(owner, repo, path, pat, ref=None):
    """
    Return {'content': parsed, 'sha': sha, 'raw': raw_string}
    If ref provided, fetch from that branch/ref.
    """
    url = f"{GITHUB_API}/repos/{owner}/{repo}/contents/{path}"
    params = {}
    if ref:
        params['ref'] = ref
    r = requests.get(url, headers=_headers(pat), params=params)
    r.raise_for_status()
    data = r.json()
    sha = data.get('sha')
    content_b64 = data.get('content','')
    parsed = None
    raw = ''
    if data.get('encoding') == 'base64':
        raw = base64.b64decode(content_b64).decode('utf-8')
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = raw
    else:
        raw = data.get('content')
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = raw
    return json.dumps({'content': parsed, 'sha': sha, 'raw': raw})

def update_file(owner, repo, path, pat, new_content_obj, sha, message="Update via app", branch=None):
    """
    Update file. branch optional.
    Accepts either Python dict/list or JSON string from Java.
    """
    # 🔹 If new_content_obj is a string (from Java .toString()), parse it first
    if isinstance(new_content_obj, str):
        try:
            new_content_obj = json.loads(new_content_obj)
        except Exception:
            # If it's not valid JSON, keep as raw string
            pass

    # Now safely dump it back to JSON
    json_str = json.dumps(new_content_obj, ensure_ascii=False, indent=2)
    content_b64 = base64.b64encode(json_str.encode("utf-8")).decode("utf-8")

    url = f"{GITHUB_API}/repos/{owner}/{repo}/contents/{path}"
    payload = {"message": message, "content": content_b64, "sha": sha}
    if branch:
        payload["branch"] = branch

    r = requests.put(url, headers=_headers(pat), json=payload)
    r.raise_for_status()
    return json.dumps(r.json())


def create_file(owner, repo, path, pat, content_obj, message="Create file via app", branch=None):
    """
    Create new file in repo. Accepts dict/list (Python) or JSON string (from Java).
    """
    # 🔹 If Java sends JSONObject.toString(), parse it
    if isinstance(content_obj, str):
        try:
            content_obj = json.loads(content_obj)
        except Exception:
            # Keep as raw string if not valid JSON
            pass

    json_str = json.dumps(content_obj, ensure_ascii=False, indent=2)
    content_b64 = base64.b64encode(json_str.encode("utf-8")).decode("utf-8")

    url = f"{GITHUB_API}/repos/{owner}/{repo}/contents/{path}"
    payload = {"message": message, "content": content_b64}
    if branch:
        payload["branch"] = branch

    r = requests.put(url, headers=_headers(pat), json=payload)
    r.raise_for_status()
    return json.dumps(r.json())


def delete_file(owner, repo, path, pat, sha, message="Delete file via app", branch=None):
    url = f"{GITHUB_API}/repos/{owner}/{repo}/contents/{path}"
    payload = {"message": message, "sha": sha}
    if branch:
        payload["branch"] = branch
    r = requests.delete(url, headers=_headers(pat), json=payload)
    r.raise_for_status()
    return json.dumps(r.json())

def list_branches(owner, repo, pat):
    url = f"{GITHUB_API}/repos/{owner}/{repo}/branches"
    r = requests.get(url, headers=_headers(pat))
    r.raise_for_status()
    return json.dumps(r.json())

def get_blob_by_sha(owner, repo, sha, pat):
    url = f"{GITHUB_API}/repos/{owner}/{repo}/git/blobs/{sha}"
    r = requests.get(url, headers=_headers(pat))
    r.raise_for_status()
    return json.dumps(r.json())
