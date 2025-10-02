from django.shortcuts import render, get_object_or_404
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from drf_yasg.utils import swagger_auto_schema

from music.services import  get_jamendo_related
from music.service.tracks import get_jamendo_track,get_random_songs, jamendo_track_search

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get song list")
def song_list(request):
    """Get list of songs from Jamendo"""
    songs = get_random_songs(limit=20)
    if not songs:
        return Response({"message": "No songs found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(songs, status=status.HTTP_200_OK)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get song details")
def song_file(request, track_id):
    #track_id = request.GET.get(id, 'id')
    song = get_jamendo_track(track_id)
    # Implement song detail logic
    if not song:
        return Response({"message": "Song not found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(song, status=status.HTTP_200_OK)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get related songs")
def related_list(request):
    """Get related songs"""
    user_id = request.GET.get('user_id', 1)
    limit = request.GET.get('limit', 10)
    related = get_jamendo_related(user_id, limit)
    if not related:
        return Response({"message": "No related songs found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(related, status=status.HTTP_200_OK)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get random songs")
def random_songs(request):
    limit = request.GET.get('limit', 10)
    songs = get_random_songs(limit=limit)
    if not songs:
        return Response({"message": "No random songs found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(songs, status=status.HTTP_200_OK)


@api_view(['GET'])
@swagger_auto_schema(operation_summary="Search tracks by name")
def track_search(request, query):
    """Search tracks by name"""
    tracks = jamendo_track_search(query)
    print(tracks)
    # if not tracks:
    #     return Response({"message": "No tracks found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(tracks.json(), status=status.HTTP_200_OK)
