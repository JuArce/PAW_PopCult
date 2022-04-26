import {useEffect, useState} from "react";
import ListService from "../../../services/ListService";
import Loader from "../errors/Loader";
import MediaInList from "../../../components/lists/MediaInList";
import Spinner from "../../../components/animation/Spinner";
import {Avatar, Chip} from "@mui/material";
import CollaborativeService from "../../../services/CollaborativeService";
import PaginationComponent from "../../../components/PaginationComponent";
import CommentSection from "../../../components/comments/CommentSection";

function ListsDescription() {
    const id = window.location.pathname.split('/')[2];
    const [list, setList] = useState(undefined);
    const [mediaInlist, setMediaInList] = useState(undefined);
    const [collabInlist, setCollabInList] = useState(undefined);
    const [page, setPage] = useState(1);
    const pageSize = 4;

    useEffect(() => {
        async function getList(id) {
            const data = await ListService.getListById(id);
            setList(data);
        }


        getList(id);
    }, [id]);

    useEffect(() => {
        if (list) {
            async function getListMedia() {
                const media = await ListService.getMediaInList({url: list.mediaUrl, page, pageSize});
                setMediaInList(media);
            }

            getListMedia();
        }
    }, [list, page, pageSize])

    useEffect(() => {
        if (list) {
            async function getListCollaborators() {
                const collabs = await CollaborativeService.getListCollaborators({
                    url: list.collaboratorsUrl,
                    page,
                    pageSize
                });
                setCollabInList(collabs);
            }

            getListCollaborators();
        }
    }, [list, page, pageSize])

    return (<>
        {list ? (<>
            <div className="flex flex-wrap pt-2">
                <h2 className="display-5 fw-bolder">
                    {list.name}
                </h2>
                {/*    report and favorite buttons */}
            </div>
            {/*    list author and forking info*/}
            {/*    forked from*/}
            {/*amount of forks*/}
            <p className="lead text-justify max-w-full break-words pb-2">
                {list.description}
            </p>
            {/*    collaborators */}
            {(collabInlist && collabInlist.data.length > 0) && (<>
                {collabInlist.data.map((user) => {
                    return <Chip
                        label={user.username}
                        variant="outlined" color="secondary"
                        avatar={<Avatar alt={user.username} src={user.imageUrl}/>}
                        key={user.username}
                    />
                })}
            </>)}
            {/*    share and edit */}
            {(mediaInlist && mediaInlist.data.length > 0) ? (<>
                    <MediaInList media={mediaInlist.data}/>
                    <div className="flex justify-center">
                        {(mediaInlist.data.length > 0 && mediaInlist.links.last.page > 1) &&
                            <PaginationComponent page={page} lastPage={mediaInlist.links.last.page}
                                                 setPage={setPage}/>}
                    </div>
                </>) :
                <div className="flex justify-center"><Spinner/></div>}
            <CommentSection commentsUrl={list.commentsUrl}/>
        </>) : <Loader/>}
    </>);
}

export default ListsDescription;