import {useEffect, useState} from "react";
import UserService from "../../services/UserService";
import {Link} from "react-router-dom";
import {ListItem} from "@mui/material";

const CommentComponent = (props) => {
    const [user, setUser] = useState(undefined);
    const [comment, setComment] = useState(undefined);

    useEffect(() => {
        setComment(props.comment);
    }, [props.comment]);

    useEffect(() => {
        if (comment) {
            async function getUserByUrl() {
                const data = await UserService.getUser(comment.userUrl);
                setUser(data);
            }

            getUserByUrl();
        }
    }, [comment]);

    return (
        <>{(comment && user) &&
            <ListItem className="p-1 my-2 ring-2 ring-gray-200 bg-white rounded-lg flex flex-wrap flex-col">
                <div className="grid grid-cols-12 gap-2">
                    <div><img className="inline-block object-cover rounded-full" alt="profile_image"
                              src={user.imageUrl}/>
                    </div>
                    <div className="col-span-10 flex flex-row items-center text-lg">
                        <Link className="text-decoration-none text-violet-500 hover:text-violet-900"
                              to={'/user/' + user.username}>{user.username}</Link>
                        <div className="text-base tracking-tight pl-1 text-gray-400">
                            &#8226;
                        </div>
                        <div className="text-base tracking-tight pl-1 text-gray-400">
                            {(comment.creationDate).slice(0, 10)}
                        </div>
                    </div>
                    <div>x</div>
                    <div></div>
                    <div className="col-span-11">
                        <p className="max-w-full break-words m-0"> {comment.commentBody} </p>
                    </div>

                </div>
            </ListItem>}</>
    );
}
export default CommentComponent;