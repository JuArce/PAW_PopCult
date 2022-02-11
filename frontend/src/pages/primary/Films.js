import MediaSlider from "../../components/media/MediaSlider";
import {useTranslation} from "react-i18next";

const DUMMY_DATA = [
    {
        id: 1,
        image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrdPsGJEBxBev7gKo_EMp0Pgk7Q7su_xTUxf3vo8dE9S_CiG2Z',
        title: 'Spiderman: No Way Home',
        releaseDate: '16/12/2021'
    },
    {
        id: 2,
        image: 'https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcQr52xgKbOq-XJuwQEknttmudqatCNYwXUIg3O4k02E6eNZmPXd',
        title: 'Clifford The Red Big Dog',
        releaseDate: '09/12/2021'
    },
    {
        id: 3,
        image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrdPsGJEBxBev7gKo_EMp0Pgk7Q7su_xTUxf3vo8dE9S_CiG2Z',
        title: 'Spiderman: No Way Home',
        releaseDate: '16/12/2021'
    },
    {
        id: 4,
        image: 'https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcQr52xgKbOq-XJuwQEknttmudqatCNYwXUIg3O4k02E6eNZmPXd',
        title: 'Clifford The Red Big Dog',
        releaseDate: '09/12/2021'
    },
    {
        id: 5,
        image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrdPsGJEBxBev7gKo_EMp0Pgk7Q7su_xTUxf3vo8dE9S_CiG2Z',
        title: 'Spiderman: No Way Home',
        releaseDate: '16/12/2021'
    },
    {
        id: 6,
        image: 'https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcQr52xgKbOq-XJuwQEknttmudqatCNYwXUIg3O4k02E6eNZmPXd',
        title: 'Clifford The Red Big Dog',
        releaseDate: '09/12/2021'
    },
];

const Films = () => {
    const { t, i18n } = useTranslation();
    return (
        <section>
            <h4 className="font-bold text-2xl pt-2">
                {t('popular_films')}</h4>
            <MediaSlider media={DUMMY_DATA}/>
        </section>
    );
}

export default Films;